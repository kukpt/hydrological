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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;


public class EndpointMessageStoreVerticle extends AbstractVerticle {

  private static final DateTimeFormatter DAY_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

  private static final int DEFAULT_READ_LIMIT = 100;

  private static final int RETENTION_DAYS = 30;

  private static final String DEFAULT_BASE_DIR =
      System.getProperty("user.dir") + "/file-uploads";

  private final String baseDir;

  private final ZoneId zoneId = ZoneId.of("Asia/Shanghai");

  public EndpointMessageStoreVerticle() {
    this(DEFAULT_BASE_DIR);
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
    vertx.setPeriodic(24L * 60L * 60L * 1000L, id -> cleanupExpiredFiles());
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
    FileSystem fs = vertx.fileSystem();
    String directory = endpointDir(endpointId);
    String filePath = dataFilePath(endpointId, messageDate);
    Buffer record = encodeRecord(payload);
    return fs.mkdirs(directory)
             .compose(unused -> fs.open(filePath, new OpenOptions().setCreate(true).setAppend(true)))
             .compose(asyncFile -> asyncFile
                 .write(record)
                 .compose(unused -> asyncFile.close()));
  }

  private Future<JsonArray> readLatest(String endpointId, int limit) {
    FileSystem fs = vertx.fileSystem();
    String filePath = dataFilePath(endpointId, LocalDate.now(zoneId));
    return fs.exists(filePath).compose(exists -> {
      if (!exists) {
        return Future.succeededFuture(new JsonArray());
      }
      return fs.readFile(filePath)
               .map(this::decodeRecords)
               .map(records -> {
                 List<JsonObject> collected = new ArrayList<>();
                 for (int i = records.size() - 1; i >= 0 && collected.size() < limit; i--) {
                   collected.add(records.get(i));
                 }
                 return new JsonArray(collected);
               });
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
        break;
      }
      String json = buffer.getString(pos, pos + length, StandardCharsets.UTF_8.name());
      pos += length;
      records.add(new JsonObject(json));
    }
    return records;
  }

  private Buffer encodeRecord(JsonObject payload) {
    Buffer json = Buffer.buffer(payload.encode());
    return Buffer.buffer().appendInt(json.length()).appendBuffer(json);
  }

  private JsonObject toEndpointInfo(String path, io.vertx.core.file.FileProps props) {
    if (!props.isDirectory()) {
      return null;
    }
    int slash = path.lastIndexOf('/') >= 0 ? path.lastIndexOf('/') : path.lastIndexOf('\\');
    String endpointId = slash >= 0 ? path.substring(slash + 1) : path;
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
    int slash = path.lastIndexOf('/') >= 0 ? path.lastIndexOf('/') : path.lastIndexOf('\\');
    String file = slash >= 0 ? path.substring(slash + 1) : path;
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
    return baseDir + "/" + endpointId;
  }

  private String dataFilePath(String endpointId, LocalDate date) {
    return endpointDir(endpointId) + "/" + date.format(DAY_FORMATTER) + ".bin";
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
