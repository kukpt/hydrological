package io.github.kukpt.sl651;


import io.github.kukpt.sl651.codec.UpstreamMessage;
import io.github.kukpt.sl651.codec.DownstreamMessage;
import io.vertx.core.Promise;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;

import java.util.Objects;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;

public class ReplyPromise {

  private final Logger logger = LoggerFactory.getLogger(getClass());


  private final Map<ReplyKey, Promise<UpstreamMessage>> PROMISE_MAP = new ConcurrentHashMap<>();

  public Promise<UpstreamMessage> setPromise(DownstreamMessage message) {
    ReplyKey key = ReplyKey.of(message);
    logger.info("Setting up promise, key: " + key);
    Promise<UpstreamMessage> promise = Promise.promise();
    Promise<UpstreamMessage> existing = PROMISE_MAP.putIfAbsent(key, promise);
    if (existing != null) {
      promise.fail("Pending reply already exists, key: " + key);
    }
    return promise;
  }

  public void clear(DownstreamMessage message, Throwable cause) {
    if (cause instanceof TimeoutException) {
      logger.info("Timeout caught in promise, key: " + ReplyKey.of(message));
    }
    PROMISE_MAP.remove(ReplyKey.of(message));
  }

  public void setReply(UpstreamMessage msg) {
    ReplyKey key = ReplyKey.of(msg);
    Promise<UpstreamMessage> promise = PROMISE_MAP.remove(key);
    if (promise == null && msg.streamId() != 0) {
      promise = PROMISE_MAP.remove(ReplyKey.withoutStreamId(msg));
    }
    if (promise != null) {
      logger.info("Reply promise for key: " + key);
      promise.complete(msg);
    }
  }

  private static final class ReplyKey {

    private final String telemetryStationAddress;

    private final int functionType;

    private final int streamId;

    private ReplyKey(String telemetryStationAddress, int functionType, int streamId) {
      this.telemetryStationAddress = telemetryStationAddress;
      this.functionType = functionType;
      this.streamId = streamId;
    }

    static ReplyKey of(DownstreamMessage message) {
      return new ReplyKey(
        message.messageHeader().telemetryStationAddress(),
        message.functionType(),
        message.streamId());
    }

    static ReplyKey of(UpstreamMessage message) {
      return new ReplyKey(
        message.telemetryStationAddress(),
        message.functionType(),
        message.streamId());
    }

    static ReplyKey withoutStreamId(UpstreamMessage message) {
      return new ReplyKey(
        message.telemetryStationAddress(),
        message.functionType(),
        0);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof ReplyKey)) {
        return false;
      }
      ReplyKey replyKey = (ReplyKey) o;
      return functionType == replyKey.functionType
        && streamId == replyKey.streamId
        && Objects.equals(telemetryStationAddress, replyKey.telemetryStationAddress);
    }

    @Override
    public int hashCode() {
      return Objects.hash(telemetryStationAddress, functionType, streamId);
    }

    @Override
    public String toString() {
      return "ReplyKey{"
        + "telemetryStationAddress='" + telemetryStationAddress + '\''
        + ", functionType=" + functionType
        + ", streamId=" + streamId
        + '}';
    }
  }
}
