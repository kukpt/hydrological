package io.github.kukpt.sl651.codec;

import io.github.kukpt.sl651.codec.element.ElementResult;
import io.github.kukpt.sl651.message.FixedBodyMessage;
import io.github.kukpt.sl651.message.ElementMessage;
import io.github.kukpt.sl651.message.IMessageBody;
import io.github.kukpt.sl651.message.LinkKeepMessage;
import io.github.kukpt.sl651.message.PeriodMessage;
import io.github.kukpt.sl651.message.PictureMessage;
import io.netty.buffer.ByteBufUtil;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.Collection;

/** Stable, storage-oriented JSON projection of an already decoded SL651 message. */
public final class Sl651ParsedDataJson {

  private Sl651ParsedDataJson() { }

  public static JsonObject encode(UpstreamMessage message, IMessageBody body) {
    MessageHeader header = message.header();
    JsonObject result = new JsonObject()
      .put("messageType", body == null ? "RawPayload" : body.getClass().getSimpleName())
      .put("header", new JsonObject()
        .put("centralStationAddress", header.centralStationAddress() & 0xFF)
        .put("telemetryStationAddress", header.telemetryStationAddress())
        .put("functionType", header.functionType())
        .put("remainingLength", header.remainingLength())
        .put("multiPack", header.multiPack()));
    if (header.multiPack()) {
      result.getJsonObject("header")
        .put("packageIndex", header.currentPackage())
        .put("packageCount", header.totalPackage());
    }
    result.put("body", bodyJson(message, body));
    return result;
  }

  /** Extracts picture metadata without copying the image bytes into the monitoring snapshot. */
  public static JsonObject encodePictureMetadata(UpstreamMessage message) {
    FixedBodyMessage fixed = new FixedBodyMessage(message.payload().duplicate());
    JsonObject result = encode(message, fixed);
    result.put("messageType", "PictureMessage");
    result.getJsonObject("body").put("picturePayloadLength", message.payload().readableBytes());
    return result;
  }

  private static JsonObject bodyJson(UpstreamMessage message, IMessageBody body) {
    if (body == null) {
      return new JsonObject().put("payloadLength", message.payload().readableBytes());
    }
    JsonObject json = new JsonObject().put("streamId", body.streamId());
    if (body instanceof LinkKeepMessage) {
      return json.put("reportTime", ((LinkKeepMessage) body).reportTime().toString());
    }
    if (body instanceof PumpStationControlResponseMessage) {
      PumpStationControlResponseMessage response = (PumpStationControlResponseMessage) body;
      return json
        .put("reportTime", response.reportTime().toString())
        .put("telemetryStationAddress", response.telemetryStationAddress())
        .put("length", response.length() & 0xFF)
        .put("command", response.command() & 0xFF);
    }
    if (body instanceof FixedBodyMessage) {
      FixedBodyMessage fixed = (FixedBodyMessage) body;
      json
        .put("reportTime", fixed.reportTime().toString())
        .put("telemetryStationAddress", fixed.telemetryStationAddress())
        .put("classificationCode", fixed.classificationCode())
        .put("observationTime", fixed.observationTime().toString());
    }
    if (body instanceof PeriodMessage) {
      PeriodMessage period = (PeriodMessage) body;
      json.put("timeStep", period.timeStep().toString())
        .put("elementId", hexId(period.elementId().id()));
    }
    if (body instanceof PictureMessage) {
      return json.put("pictureLength", ((PictureMessage) body).getPicture().length());
    }
    if (body instanceof ElementMessage) {
      json.put("elements", encodeElements(((ElementMessage) body).elementResults()));
    }
    return json;
  }

  private static JsonArray encodeElements(Collection<ElementResult> elements) {
    JsonArray array = new JsonArray();
    for (ElementResult element : elements) {
      array.add(new JsonObject()
        .put("elementId", hexId(element.elementId().id()))
        .put("valueType", element.valueType().name())
        .put("value", elementValue(element)));
    }
    return array;
  }

  private static Object elementValue(ElementResult element) {
    switch (element.valueType()) {
      case STATUS:
        return element.statusValue();
      case DOUBLE:
        return element.doubleValue();
      case STRING:
        return element.stringValue();
      case DOUBLE_ARRAY:
        JsonArray doubles = new JsonArray();
        for (double value : element.doubleArrayValue()) doubles.add(value);
        return doubles;
      case BYTE_ARRAY:
        return ByteBufUtil.hexDump(element.byteArrayValue());
      case TIME_STEP:
        return element.timeStepValue().toString();
      default:
        return element.value();
    }
  }

  private static String hexId(int id) {
    return String.format("0x%02X", id);
  }
}
