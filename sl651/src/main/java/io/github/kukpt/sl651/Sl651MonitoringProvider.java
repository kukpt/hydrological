package io.github.kukpt.sl651;

import io.github.kukpt.hydrological.monitoring.ProtocolMonitoringProvider;
import io.github.kukpt.sl651.codec.PumpStationControlResponseMessage;
import io.github.kukpt.sl651.codec.RawDownstreamPayload;
import io.github.kukpt.sl651.utils.LocalEbTopic;
import io.netty.buffer.ByteBufUtil;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.NoSuchElementException;

/** SL651-specific operations exposed through the shared monitoring Web. */
public final class Sl651MonitoringProvider implements ProtocolMonitoringProvider {
  private final Vertx vertx;
  private final OnlineEndpointRegistry onlineEndpoints;

  public Sl651MonitoringProvider(Vertx vertx, OnlineEndpointRegistry onlineEndpoints) {
    this.vertx = vertx;
    this.onlineEndpoints = onlineEndpoints;
  }

  @Override
  public String protocol() {
    return "SL651";
  }

  @Override
  public JsonArray onlineEndpoints() {
    return onlineEndpoints.toJson();
  }

  @Override
  public Future<JsonObject> execute(String action, JsonObject body) {
    switch (action) {
      case "downstream/raw":
        return rawDownstream(body);
      case "downstream/pump-control":
        return pumpControl(body);
      case "debug/start":
        return setDebug(body, true);
      case "debug/stop":
        return setDebug(body, false);
      default:
        return ProtocolMonitoringProvider.super.execute(action, body);
    }
  }

  private Future<JsonObject> rawDownstream(JsonObject body) {
    String endpointId = requiredText(body, "endpointId");
    String payloadHex = requiredText(body, "payloadHex");
    Integer functionType = body.getInteger("functionType");
    Integer frameControlType = body.getInteger("frameControlType", 0x05);
    Long timeout = body.getLong("timeout", 30L);
    if (endpointId == null || payloadHex == null) {
      return invalid("endpointId and payloadHex are required");
    }
    if (functionType == null || functionType < 0 || functionType > 0xFF) {
      return invalid("functionType must be between 0 and 255");
    }
    if (frameControlType < 0 || frameControlType > 0xFF) {
      return invalid("frameControlType must be between 0 and 255");
    }
    if (!validTimeout(timeout)) return invalid("timeout must be between 1 and 300 seconds");
    String normalizedHex = payloadHex.replaceAll("\\s+", "");
    if (normalizedHex.isEmpty() || (normalizedHex.length() & 1) != 0
        || !normalizedHex.matches("[0-9a-fA-F]+")) {
      return invalid("payloadHex must contain an even number of hexadecimal characters");
    }
    Sl651Endpoint endpoint = endpoint(endpointId);
    if (endpoint == null) return offline(endpointId);
    return endpoint.downstream(functionType, RawDownstreamPayload.fromHex(normalizedHex),
        frameControlType.shortValue(), timeout)
      .map(response -> new JsonObject()
        .put("success", true)
        .put("protocol", protocol())
        .put("endpointId", endpointId)
        .put("functionType", response.functionType())
        .put("streamId", response.streamId())
        .put("responsePayloadHex", ByteBufUtil.hexDump(response.payload())));
  }

  private Future<JsonObject> pumpControl(JsonObject body) {
    String endpointId = requiredText(body, "endpointId");
    Integer command = body.getInteger("command");
    Long timeout = body.getLong("timeout", 30L);
    if (endpointId == null || command == null) {
      return invalid("endpointId and command are required");
    }
    if (command < 0 || command > 0xFF) {
      return invalid("command must be between 0 and 255");
    }
    if (!validTimeout(timeout)) return invalid("timeout must be between 1 and 300 seconds");
    Sl651Endpoint endpoint = endpoint(endpointId);
    if (endpoint == null) return offline(endpointId);
    return endpoint.pumpStationControl(command.shortValue(), timeout)
      .map(response -> pumpResponse(endpointId, response));
  }

  private Future<JsonObject> setDebug(JsonObject body, boolean enabled) {
    String endpointId = requiredText(body, "endpointId");
    if (endpointId == null) return invalid("endpointId is required");
    vertx.eventBus().send(enabled
      ? LocalEbTopic.generateEnableEndpointDebugTopic(endpointId)
      : LocalEbTopic.generateDisableEndpointDebugTopic(endpointId), new JsonObject());
    return Future.succeededFuture(new JsonObject()
      .put("success", true).put("protocol", protocol())
      .put("endpointId", endpointId).put("debug", enabled));
  }

  private Sl651Endpoint endpoint(String endpointId) {
    return endpointId == null ? null : onlineEndpoints.get(endpointId);
  }

  private static JsonObject pumpResponse(
      String endpointId, PumpStationControlResponseMessage response) {
    return new JsonObject().put("success", true).put("protocol", "SL651")
      .put("endpointId", endpointId)
      .put("telemetryStationAddress", response.telemetryStationAddress())
      .put("streamId", response.streamId())
      .put("reportTime", response.reportTime().toString())
      .put("length", response.length()).put("command", response.command());
  }

  private static String requiredText(JsonObject body, String field) {
    String value = body.getString(field);
    return value == null || value.trim().isEmpty() ? null : value.trim();
  }

  private static boolean validTimeout(Long timeout) {
    return timeout != null && timeout >= 1 && timeout <= 300;
  }

  private static Future<JsonObject> invalid(String message) {
    return Future.failedFuture(new IllegalArgumentException(message));
  }

  private static Future<JsonObject> offline(String endpointId) {
    return Future.failedFuture(new NoSuchElementException("endpoint is offline: " + endpointId));
  }
}
