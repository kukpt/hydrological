package io.github.kukpt.sl651;

import io.github.kukpt.sl651.codec.UpstreamMessage;
import io.vertx.core.Promise;

import java.util.HashMap;
import java.util.Map;

public class ReplyPromise {

  private final Map<Integer, Promise<UpstreamMessage>> PROMISE_MAP = new HashMap<>();

  public Promise<UpstreamMessage> setPromise(int functionType) {
    Promise<UpstreamMessage> promise = Promise.promise();
    PROMISE_MAP.put(functionType, promise);
    return promise;
  }

  public void setReply( UpstreamMessage msg) {
    // 设置
    if (PROMISE_MAP.containsKey(msg.functionType())) {
      PROMISE_MAP.remove(msg.functionType()).complete(msg);
    }
  }

}
