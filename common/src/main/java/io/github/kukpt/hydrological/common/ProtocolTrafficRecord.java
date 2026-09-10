package io.github.kukpt.hydrological.common;

import io.vertx.core.json.JsonObject;

import java.time.LocalDateTime;
import java.util.Objects;

/** Protocol-neutral raw traffic record used by all hydrological codecs. */
public class ProtocolTrafficRecord {
  private final ProtocolType protocol;
  private final TrafficDirection type;
  private final String endpointId;
  private final String remoteAddr;
  private final String payload;
  private final String parseStatus;
  private final JsonObject parsedData;
  private final String parseError;
  private final LocalDateTime recordedTime;

  public ProtocolTrafficRecord(ProtocolType protocol, TrafficDirection type, String endpointId,
                               String remoteAddr, String payload) {
    this(protocol, type, endpointId, remoteAddr, payload, null, null, null,
      LocalDateTime.now());
  }

  public ProtocolTrafficRecord(ProtocolType protocol, TrafficDirection type, String endpointId,
                               String remoteAddr, String payload, LocalDateTime recordedTime) {
    this(protocol, type, endpointId, remoteAddr, payload, null, null, null, recordedTime);
  }

  public ProtocolTrafficRecord(ProtocolType protocol, TrafficDirection type, String endpointId,
                               String remoteAddr, String payload, String parseStatus,
                               JsonObject parsedData, String parseError) {
    this(protocol, type, endpointId, remoteAddr, payload, parseStatus, parsedData, parseError,
      LocalDateTime.now());
  }

  public ProtocolTrafficRecord(ProtocolType protocol, TrafficDirection type, String endpointId,
                               String remoteAddr, String payload, String parseStatus,
                               JsonObject parsedData, String parseError,
                               LocalDateTime recordedTime) {
    this.protocol = Objects.requireNonNull(protocol, "protocol");
    this.type = Objects.requireNonNull(type, "type");
    this.endpointId = endpointId;
    this.remoteAddr = remoteAddr;
    this.payload = Objects.requireNonNull(payload, "payload");
    this.parseStatus = parseStatus;
    this.parsedData = parsedData;
    this.parseError = parseError;
    this.recordedTime = Objects.requireNonNull(recordedTime, "recordedTime");
  }

  public ProtocolType getProtocol() { return protocol; }
  public TrafficDirection getType() { return type; }
  public String getEndpointId() { return endpointId; }
  public String getRemoteAddr() { return remoteAddr; }
  public String getPayload() { return payload; }
  public String getParseStatus() { return parseStatus; }
  public JsonObject getParsedData() { return parsedData == null ? null : parsedData.copy(); }
  public String getParseError() { return parseError; }
  public LocalDateTime getRecordedTime() { return recordedTime; }

  public JsonObject toJson() {
    JsonObject json = new JsonObject()
      .put("protocol", protocol.name())
      .put("type", type.name())
      .put("endpointId", endpointId)
      .put("remoteAddr", remoteAddr)
      .put("payload", payload)
      .put("recordedTime", recordedTime.toString());
    if (parseStatus != null) json.put("parseStatus", parseStatus);
    if (parsedData != null) json.put("parsedData", parsedData);
    if (parseError != null) json.put("parseError", parseError);
    return json;
  }

  @Override
  public String toString() {
    return "ProtocolTrafficRecord{" +
      "protocol=" + protocol +
      ", type=" + type +
      ", endpointId='" + endpointId + '\'' +
      ", remoteAddr='" + remoteAddr + '\'' +
      ", payload='" + payload + '\'' +
      ", parseStatus='" + parseStatus + '\'' +
      ", parsedData=" + parsedData +
      ", parseError='" + parseError + '\'' +
      ", recordedTime=" + recordedTime +
      '}';
  }
}
