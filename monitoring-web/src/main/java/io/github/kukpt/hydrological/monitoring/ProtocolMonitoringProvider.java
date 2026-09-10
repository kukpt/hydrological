package io.github.kukpt.hydrological.monitoring;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/** Protocol-specific data and operations exposed by the shared monitoring Web. */
public interface ProtocolMonitoringProvider {
  String protocol();

  JsonArray onlineEndpoints();

  default Future<JsonObject> execute(String action, JsonObject request) {
    return Future.failedFuture(new UnsupportedOperationException(
      protocol() + " does not support monitoring action " + action));
  }
}
