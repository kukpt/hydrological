package io.github.kukpt.sl651.metrics;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.github.kukpt.sl651.HydrologicalEndpoint;

import java.time.LocalDateTime;

public class TrafficMonitor {

  public TrafficMonitor(TYPE type, HydrologicalEndpoint endpoint, String payload) {
    this.type = type;
    this.endpointId = endpoint.endpointId();
    this.remoteAddr = endpoint.remoteAddress().toString();
    this.payload = payload;
    this.recordedTime = LocalDateTime.now();
  }

  public enum TYPE {
    INBOUND,
    OUTBOUND;
  }

  public TYPE type;

  private String endpointId;

  private String remoteAddr;

  private String payload;

  @JsonFormat(
  pattern = "yyyy-MM-dd HH:mm:ss.SSS",
  timezone = "UTC")
  private LocalDateTime recordedTime;

  public TYPE getType() {
    return type;
  }

  public String getEndpointId() {
    return endpointId;
  }

  public String getRemoteAddr() {
    return remoteAddr;
  }

  public String getPayload() {
    return payload;
  }

  public LocalDateTime getRecordedTime() {
    return recordedTime;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("TrafficMonitor{");
    sb.append("type=").append(type);
    sb.append(", endpointId='").append(endpointId).append('\'');
    sb.append(", remoteAddr='").append(remoteAddr).append('\'');
    sb.append(", payload='").append(payload).append('\'');
    sb.append(", recordedTime=").append(recordedTime);
    sb.append('}');
    return sb.toString();
  }
}
