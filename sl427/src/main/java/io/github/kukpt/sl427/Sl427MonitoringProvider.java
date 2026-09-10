package io.github.kukpt.sl427;

import io.github.kukpt.hydrological.monitoring.ProtocolMonitoringProvider;
import io.vertx.core.json.JsonArray;

/** Read-only SL427 view exposed through the shared monitoring Web. */
public final class Sl427MonitoringProvider implements ProtocolMonitoringProvider {
  private final Sl427OnlineEndpointRegistry onlineEndpoints;

  public Sl427MonitoringProvider(Sl427OnlineEndpointRegistry onlineEndpoints) {
    this.onlineEndpoints = onlineEndpoints;
  }

  @Override
  public String protocol() {
    return "SL427";
  }

  @Override
  public JsonArray onlineEndpoints() {
    return onlineEndpoints.toJson();
  }
}
