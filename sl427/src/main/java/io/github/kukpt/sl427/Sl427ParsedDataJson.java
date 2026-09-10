package io.github.kukpt.sl427;

import io.github.kukpt.sl427.buffer.Hex;
import io.github.kukpt.sl427.frame.Sl427Frame;
import io.github.kukpt.sl427.frame.TimeTag;
import io.github.kukpt.sl427.model.CommandMessage;
import io.github.kukpt.sl427.model.LinkDetectionMessage;
import io.github.kukpt.sl427.model.Measurement;
import io.github.kukpt.sl427.model.ProtocolMessage;
import io.github.kukpt.sl427.model.RawAfnMessage;
import io.github.kukpt.sl427.model.RealtimeDataMessage;
import io.github.kukpt.sl427.model.StatusField;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/** Stable, storage-oriented JSON projection of an already parsed SL427 message. */
public final class Sl427ParsedDataJson {

  private Sl427ParsedDataJson() { }

  public static JsonObject encode(ProtocolMessage message) {
    JsonObject json = new JsonObject()
      .put("messageType", message.getClass().getSimpleName())
      .put("frame", frameJson(message.frame()));
    if (message instanceof RealtimeDataMessage) {
      RealtimeDataMessage realtime = (RealtimeDataMessage) message;
      JsonArray measurements = new JsonArray();
      for (Measurement measurement : realtime.measurements()) {
        measurements.add(new JsonObject()
          .put("pointId", measurement.pointId())
          .put("parameterType", measurement.type().name())
          .put("value", measurement.value())
          .put("unit", measurement.unit())
          .put("quality", measurement.quality().name()));
      }
      JsonObject body = new JsonObject()
        .put("parameterType", realtime.parameterType().name())
        .put("measurements", measurements);
      if (realtime.status() != null) body.put("status", statusJson(realtime.status()));
      if (realtime.timeTag() != null) body.put("timeTag", timeTagJson(realtime.timeTag()));
      return json.put("body", body);
    }
    if (message instanceof LinkDetectionMessage) {
      LinkDetectionMessage link = (LinkDetectionMessage) message;
      return json.put("body", new JsonObject()
        .put("linkType", link.type().name())
        .put("linkCode", link.type().code()));
    }
    if (message instanceof CommandMessage) {
      CommandMessage command = (CommandMessage) message;
      byte[] data = command.data();
      JsonObject body = new JsonObject()
        .put("role", command.role().name())
        .put("dataLength", data.length)
        .put("dataHex", Hex.encode(data));
      if (command.timeTag() != null) body.put("timeTag", timeTagJson(command.timeTag()));
      return json.put("body", body);
    }
    if (message instanceof RawAfnMessage) {
      Sl427Frame frame = message.frame();
      int bodyLength = frame.declaredLength() - 1 - 5 - frame.afn().byteLength();
      return json.put("body", new JsonObject().put("bodyLength", Math.max(0, bodyLength)));
    }
    return json;
  }

  private static JsonObject frameJson(Sl427Frame frame) {
    return new JsonObject()
      .put("address", frame.address().canonicalKey())
      .put("addressMode", frame.address().mode().name())
      .put("afn", frame.afn().toString())
      .put("control", frame.control().raw())
      .put("direction", frame.control().direction().name())
      .put("split", frame.control().split())
      .put("splitCountdown", frame.splitCountdown())
      .put("declaredLength", frame.declaredLength());
  }

  private static JsonObject statusJson(StatusField status) {
    return new JsonObject()
      .put("alarmBits", status.alarmBits())
      .put("terminalBits", status.terminalBits());
  }

  private static JsonObject timeTagJson(TimeTag timeTag) {
    return new JsonObject()
      .put("timestamp", timeTag.timestamp().toString())
      .put("delayMinutes", timeTag.delayMinutes());
  }
}
