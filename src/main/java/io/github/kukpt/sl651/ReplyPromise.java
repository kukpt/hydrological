package io.github.kukpt.sl651;

import io.github.kukpt.sl651.codec.UpstreamMessage;
import io.vertx.core.Promise;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;

public class ReplyPromise {

  private final Logger logger = LoggerFactory.getLogger(getClass());


  private final Map<Integer, Promise<UpstreamMessage>> PROMISE_MAP = new ConcurrentHashMap<>();

  public Promise<UpstreamMessage> setPromise(int functionType) {
    logger.info("Setting up promise, functionType: " + functionType);
    Promise<UpstreamMessage> promise = Promise.promise();
    PROMISE_MAP.put(functionType, promise);
    return promise;
  }

  public void onTimeOutClear(int functionType, Throwable cause) {
    if (cause instanceof TimeoutException) {
      logger.info("Timeout caught in promise, functionType: " + functionType);
      PROMISE_MAP.remove(functionType);
    }
  }

  public void setReply(UpstreamMessage msg) {
    // 设置
    if (PROMISE_MAP.containsKey(msg.functionType())) {
      logger.info("Reply promise for functionType: " + msg.functionType());
      PROMISE_MAP.remove(msg.functionType()).complete(msg);
    }
  }

}
