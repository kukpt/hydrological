package io.github.kukpt.sl651.metrics;

import io.github.kukpt.hydrological.common.ProtocolTrafficRecord;
import io.github.kukpt.hydrological.common.ProtocolType;
import io.github.kukpt.hydrological.common.TrafficDirection;
import io.github.kukpt.sl651.Sl651Endpoint;
import io.vertx.core.json.JsonObject;

import java.time.LocalDateTime;

/** @deprecated use {@link ProtocolTrafficRecord}; retained for source compatibility. */
@Deprecated
public class TrafficMonitor {

  private final ProtocolTrafficRecord delegate;

  public TrafficMonitor(TYPE type, Sl651Endpoint endpoint, String payload) {
    this.type = type;
    this.delegate = new ProtocolTrafficRecord(ProtocolType.SL651,
      TrafficDirection.valueOf(type.name()), endpoint.endpointId(), endpoint.remoteAddress().toString(), payload);
  }

  public enum TYPE {
    INBOUND,
    OUTBOUND;
  }

  public TYPE type;

  public TYPE getType() {
    return type;
  }

  public String getEndpointId() {
    return delegate.getEndpointId();
  }

  public String getRemoteAddr() {
    return delegate.getRemoteAddr();
  }

  public String getPayload() {
    return delegate.getPayload();
  }

  public LocalDateTime getRecordedTime() {
    return delegate.getRecordedTime();
  }

  public JsonObject toJson() {
    return delegate.toJson();
  }

  @Override
  public String toString() {
    return delegate.toString();
  }
}
