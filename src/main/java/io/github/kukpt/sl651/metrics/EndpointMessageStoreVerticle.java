package io.github.kukpt.sl651.metrics;

import io.github.kukpt.sl651.utils.LocalEbTopic;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.Message;
import io.vertx.core.file.FileSystem;
import io.vertx.core.file.OpenOptions;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;


public class EndpointMessageStoreVerticle extends AbstractVerticle {

  private static final DateTimeFormatter DAY_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

  private static final int DEFAULT_READ_LIMIT = 100;

  private static final int RETENTION_DAYS = 30;


  private String baseDir;

  private final ZoneId zoneId = ZoneId.of("Asia/Shanghai");

  // 每个文件一个写入队列，确保串行化
  private final ConcurrentHashMap<String, Queue<WriteTask>> writeQueues = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, Boolean> writeInProgress = new ConcurrentHashMap<>();

  private static class WriteTask {
    final Buffer record;
    final Promise<Void> promise;

    WriteTask(Buffer record, Promise<Void> promise) {
      this.record = record;
      this.promise = promise;
    }
  }

  public EndpointMessageStoreVerticle(String baseDir) {
    this.baseDir = Objects.requireNonNull(baseDir, "baseDir");
  }

  private final static Logger log = LoggerFactory.getLogger(EndpointMessageStoreVerticle.class);

  @Override
  public void start(Promise<Void> startPromise) {
    FileSystem fs = vertx.fileSystem();
    vertx.eventBus().<JsonObject>consumer(LocalEbTopic.endpointMessageAppendTopic(), this::handleAppend);
    vertx.eventBus().<JsonObject>consumer(LocalEbTopic.endpointMessageReadTopic(), this::handleRead);
    vertx.eventBus().<JsonObject>consumer(LocalEbTopic.endpointMessageListTopic(), this::handleList);

    // 每24小时清理过期文件和队列
    vertx.setPeriodic(24L * 60L * 60L * 1000L, id -> {
      cleanupExpiredFiles();
      cleanupExpiredQueues();
    });

    // 每小时清理空闲队列（防止内存泄漏）
    vertx.setPeriodic(60L * 60L * 1000L, id -> cleanupIdleQueues());

    fs.mkdirs(baseDir)
      .compose(unused -> cleanupExpiredFiles())
      .onComplete(startPromise);
  }


  private void handleAppend(Message<JsonObject> message) {
    JsonObject body = message.body();
    if (body == null) {
      message.fail(400, "message body is required");
      return;
    }
    String endpointId = sanitizeEndpointId(body.getString("endpointId"));
    if (endpointId == null) {
      message.fail(400, "endpointId is required");
      return;
    }
    JsonObject payload = body.copy();
    LocalDate messageDate = resolveMessageDate(payload);
    payload.put("_storedAt", LocalDateTime.now(zoneId).toString());
    appendRecord(endpointId, messageDate, payload)
//        .compose(unused -> cleanupExpiredFiles())
.onSuccess(unused -> message.reply(new JsonObject()
    .put("endpointId", endpointId)
    .put("date", messageDate.format(DAY_FORMATTER))
    .put("file", dataFilePath(endpointId, messageDate))))
.onFailure(err -> message.fail(500, err.getMessage()));
  }

  private void handleRead(Message<JsonObject> message) {
    JsonObject body = message.body() == null ? new JsonObject() : message.body();
    String endpointId = sanitizeEndpointId(body.getString("endpointId"));
    if (endpointId == null) {
      message.fail(400, "endpointId is required");
      return;
    }
    int limit = body.getInteger("limit", DEFAULT_READ_LIMIT);
    if (limit <= 0) {
      message.fail(400, "limit must be greater than 0");
      return;
    }
    readLatest(endpointId, Math.min(limit, DEFAULT_READ_LIMIT))
        .onSuccess(message::reply)
        .onFailure(err -> message.fail(500, err.getMessage()));
  }

  private void handleList(Message<JsonObject> message) {
    listEndpointIds()
        .onSuccess(message::reply)
        .onFailure(err -> message.fail(500, err.getMessage()));
  }

  private Future<Void> appendRecord(String endpointId, LocalDate messageDate, JsonObject payload) {
    String directory = endpointDir(endpointId);
    String filePath = dataFilePath(endpointId, messageDate);
    Buffer record = encodeRecord(payload);

    Promise<Void> promise = Promise.promise();
    WriteTask task = new WriteTask(record, promise);

    // 将任务加入队列
    Queue<WriteTask> queue = writeQueues.computeIfAbsent(filePath, k -> new LinkedList<>());
    synchronized (queue) {
      queue.offer(task);
    }

    // 触发处理队列
    processWriteQueue(filePath, directory);

    return promise.future();
  }

  private void processWriteQueue(String filePath, String directory) {
    // 如果已经有写入在进行，直接返回
    if (!writeInProgress.computeIfAbsent(filePath, k -> false)) {
      writeInProgress.put(filePath, true);
      doProcessWriteQueue(filePath, directory);
    } else {
      // 记录队列积压情况
      Queue<WriteTask> queue = writeQueues.get(filePath);
      if (queue != null && queue.size() > 10) {
        log.warn("Write queue backlog for " + filePath + ": " + queue.size() + " tasks");
      }
    }
  }

  private void doProcessWriteQueue(String filePath, String directory) {
    Queue<WriteTask> queue = writeQueues.get(filePath);
    if (queue == null) {
      writeInProgress.put(filePath, false);
      return;
    }

    WriteTask task;
    synchronized (queue) {
      task = queue.poll();
    }

    if (task == null) {
      // 队列为空，清理资源
      writeInProgress.remove(filePath);
      writeQueues.remove(filePath);
      log.debug("Write queue cleaned up for: " + filePath);
      return;
    }

    FileSystem fs = vertx.fileSystem();
    log.debug("Appending record to: " + filePath + ", size: " + task.record.length() + " bytes, queue size: " + queue.size());

    fs.mkdirs(directory)
      .compose(unused -> {
        log.debug("Directory created/verified: " + directory);
        return fs.open(filePath, new OpenOptions()
            .setCreate(true)
            .setAppend(true)
            .setDsync(true));  // 启用数据同步（Linux fsync）
      })
      .compose(asyncFile -> asyncFile
          .write(task.record)
          .compose(unused -> {
            log.debug("Record written, flushing to disk");
            return asyncFile.flush();  // 显式刷新
          })
          .compose(unused -> {
            log.debug("Record flushed successfully");
            return asyncFile.close();
          }))
      .onSuccess(unused -> {
        task.promise.complete();
        // 继续处理队列中的下一个任务
        doProcessWriteQueue(filePath, directory);
      })
      .onFailure(err -> {
        log.error("Failed to append record to " + filePath, err);
        task.promise.fail(err);
        // 即使失败也继续处理队列
        doProcessWriteQueue(filePath, directory);
      });
  }

  private Future<JsonArray> readLatest(String endpointId, int limit) {
    FileSystem fs = vertx.fileSystem();
    String filePath = dataFilePath(endpointId, LocalDate.now(zoneId));
    log.debug("Reading from: " + filePath);
    return fs.exists(filePath).compose(exists -> {
      if (!exists) {
        log.debug("File does not exist: " + filePath);
        return Future.succeededFuture(new JsonArray());
      }
      return fs.readFile(filePath)
               .compose(buffer -> {
                 log.debug("File read, size: " + buffer.length() + " bytes");
                 return Future.succeededFuture(buffer);
               })
               .map(this::decodeRecords)
               .map(records -> {
                 log.debug("Decoded " + records.size() + " records");
                 List<JsonObject> collected = new ArrayList<>();
                 for (int i = records.size() - 1; i >= 0 && collected.size() < limit; i--) {
                   collected.add(records.get(i));
                 }
                 return new JsonArray(collected);
               })
               .onFailure(err -> log.error("Failed to read from " + filePath, err));
    });
  }

  private Future<JsonArray> listEndpointIds() {
    FileSystem fs = vertx.fileSystem();
    return fs.exists(baseDir).compose(exists -> {
      if (!exists) {
        return Future.succeededFuture(new JsonArray());
      }
      return fs.readDir(baseDir).compose(paths -> {
        if (paths.isEmpty()) {
          return Future.succeededFuture(new JsonArray());
        }
        List<Future<JsonObject>> futures = new ArrayList<>();
        for (String path : paths) {
          futures.add(fs.props(path).map(props -> toEndpointInfo(path, props)));
        }
        return Future.all(futures).map(composite -> {
          List<JsonObject> values = composite.list().stream()
                                             .map(item -> (JsonObject) item)
                                             .filter(Objects::nonNull)
                                             .sorted(Comparator.comparing(o -> o.getString("endpointId")))
                                             .collect(Collectors.toList());
          return new JsonArray(values);
        });
      });
    });
  }

  private List<JsonObject> decodeRecords(Buffer buffer) {
    List<JsonObject> records = new ArrayList<>();
    int pos = 0;
    while (pos + 4 <= buffer.length()) {
      int length = buffer.getInt(pos);
      pos += 4;
      if (length < 0 || pos + length > buffer.length()) {
        log.warn("Invalid record length: " + length + " at position " + (pos - 4) + ", buffer size: " + buffer.length());
        break;
      }
      try {
        byte[] bytes = buffer.getBytes(pos, pos + length);
        String json = new String(bytes, StandardCharsets.UTF_8);
        pos += length;
        records.add(new JsonObject(json));
      } catch (Exception e) {
        log.error("Failed to decode record at position " + pos, e);
        break;
      }
    }
    return records;
  }

  private Buffer encodeRecord(JsonObject payload) {
    byte[] jsonBytes = payload.encode().getBytes(StandardCharsets.UTF_8);
    return Buffer.buffer().appendInt(jsonBytes.length).appendBytes(jsonBytes);
  }

  private JsonObject toEndpointInfo(String path, io.vertx.core.file.FileProps props) {
    if (!props.isDirectory()) {
      return null;
    }
    String endpointId = Paths.get(path).getFileName().toString();
    return new JsonObject()
        .put("endpointId", endpointId)
        .put("path", path)
        .put("modifiedTime", props.lastModifiedTime());
  }

  private Future<Void> cleanupExpiredFiles() {
    FileSystem fs = vertx.fileSystem();
    LocalDate expireBefore = LocalDate.now(zoneId).minusDays(RETENTION_DAYS);
    return fs.exists(baseDir).compose(exists -> {
      if (!exists) {
        return Future.succeededFuture();
      }
      return fs.readDir(baseDir)
               .compose(endpointDirs -> cleanupEndpointDirs(endpointDirs, expireBefore, fs));
    });
  }

  private Future<Void> cleanupEndpointDirs(List<String> endpointDirs, LocalDate expireBefore, FileSystem fs) {
    Future<Void> future = Future.succeededFuture();
    for (String endpointDir : endpointDirs) {
      future = future.compose(unused -> fs.readDir(endpointDir)
                                          .compose(files -> cleanupFiles(files, expireBefore, fs))
                                          .compose(unused2 -> deleteDirIfEmpty(endpointDir, fs)));
    }
    return future;
  }

  private Future<Void> cleanupFiles(List<String> files, LocalDate expireBefore, FileSystem fs) {
    Future<Void> future = Future.succeededFuture();
    for (String path : files) {
      future = future.compose(unused -> {
        LocalDate fileDate = extractDate(path);
        if (fileDate != null && fileDate.isBefore(expireBefore)) {
          return fs.delete(path);
        }
        return Future.succeededFuture();
      });
    }
    return future;
  }

  private Future<Void> deleteDirIfEmpty(String endpointDir, FileSystem fs) {
    return fs.readDir(endpointDir).compose(paths -> {
      if (paths.isEmpty()) {
        return fs.delete(endpointDir);
      }
      return Future.succeededFuture();
    });
  }

  /**
   * 清理过期文件对应的队列（配合文件清理）
   */
  private void cleanupExpiredQueues() {
    LocalDate expireBefore = LocalDate.now(zoneId).minusDays(RETENTION_DAYS);
    int cleaned = 0;

    for (String filePath : new ArrayList<>(writeQueues.keySet())) {
      LocalDate fileDate = extractDate(filePath);
      if (fileDate != null && fileDate.isBefore(expireBefore)) {
        writeQueues.remove(filePath);
        writeInProgress.remove(filePath);
        cleaned++;
      }
    }

    if (cleaned > 0) {
      log.info("Cleaned up " + cleaned + " expired write queues");
    }
  }

  /**
   * 清理空闲队列（队列为空且没有写入进行中）
   */
  private void cleanupIdleQueues() {
    int cleaned = 0;

    for (String filePath : new ArrayList<>(writeQueues.keySet())) {
      Queue<WriteTask> queue = writeQueues.get(filePath);
      Boolean inProgress = writeInProgress.get(filePath);

      // 队列为空且没有写入进行中
      if (queue != null && queue.isEmpty() && (inProgress == null || !inProgress)) {
        writeQueues.remove(filePath);
        writeInProgress.remove(filePath);
        cleaned++;
      }
    }

    if (cleaned > 0) {
      log.debug("Cleaned up " + cleaned + " idle write queues, remaining: " + writeQueues.size());
    }
  }

  private LocalDate resolveMessageDate(JsonObject payload) {
    String value = payload.getString("recordedTime");
    if (value == null) {
      value = payload.getString("timestamp");
    }
    if (value == null) {
      return LocalDate.now(zoneId);
    }
    try {
      return OffsetDateTime.parse(value).atZoneSameInstant(zoneId).toLocalDate();
    } catch (DateTimeParseException ignore) {
      try {
        return LocalDateTime.parse(value).atZone(zoneId).toLocalDate();
      } catch (DateTimeParseException ignored) {
        return LocalDate.now(zoneId);
      }
    }
  }

  private LocalDate extractDate(String path) {
    String file = Paths.get(path).getFileName().toString();
    int dot = file.lastIndexOf('.');
    if (dot <= 0) {
      return null;
    }
    try {
      return LocalDate.parse(file.substring(0, dot), DAY_FORMATTER);
    } catch (DateTimeParseException ignore) {
      return null;
    }
  }

  private String endpointDir(String endpointId) {
    return Paths.get(baseDir, endpointId).toString();
  }

  private String dataFilePath(String endpointId, LocalDate date) {
    return Paths.get(baseDir, endpointId, date.format(DAY_FORMATTER) + ".bin").toString();
  }

  private String sanitizeEndpointId(String endpointId) {
    if (endpointId == null) {
      return null;
    }
    String trimmed = endpointId.trim();
    if (trimmed.isEmpty()) {
      return null;
    }
    return trimmed.replaceAll("[\\\\/:*?\"<>|]", "_");
  }
}
