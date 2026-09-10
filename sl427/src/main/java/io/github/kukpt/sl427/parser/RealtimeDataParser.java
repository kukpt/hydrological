package io.github.kukpt.sl427.parser;

import io.github.kukpt.hydrological.common.ProtocolDecodeLogger;
import io.github.kukpt.hydrological.common.ProtocolType;
import io.github.kukpt.sl427.context.ProtocolContext;
import io.github.kukpt.sl427.context.ProtocolPoint;
import io.github.kukpt.sl427.data.Sl427DataDecoders;
import io.github.kukpt.sl427.data.Sl427DataDecoders.Value;
import io.github.kukpt.sl427.frame.Sl427Frame;
import io.github.kukpt.sl427.model.Measurement;
import io.github.kukpt.sl427.model.DataQuality;
import io.github.kukpt.sl427.model.ParameterType;
import io.github.kukpt.sl427.model.ProtocolMessage;
import io.github.kukpt.sl427.model.RealtimeDataMessage;
import io.github.kukpt.sl427.model.StatusField;
import io.github.kukpt.sl427.result.ParseIssue;
import io.github.kukpt.sl427.result.ParseResult;
import io.github.kukpt.sl427.result.ParseStatus;
import io.github.kukpt.sl427.schema.BodyParts;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class RealtimeDataParser {

  private static final ProtocolDecodeLogger log =
    ProtocolDecodeLogger.getLogger(RealtimeDataParser.class, ProtocolType.SL427);

  private static final int[] WATER_QUALITY_SCALES = {
    1, 2, 2, 0, 0, 2, 2, 3, 2, 3, 1, 1, 3, 3, 3, 3,
    3, 3, 2, 2, 1, 3, 2, 3, 2, 3, 0, 2, 1, 2, 2, 2,
    0, 0, 0, 0, 0, 0, 0, 0
  };

  public ParseResult<ProtocolMessage> parse(Sl427Frame frame, BodyParts parts, ProtocolContext context) {
    ParameterType type = ParameterType.forRealtime(frame.afn().value(), frame.control().typeCode());
    if (type == null) {
      log.warn("data", "实时数据类型不支持, afn=" + frame.afn()
        + ", typeCode=" + frame.control().typeCode());
      return invalid(frame, "UNSUPPORTED_REALTIME_TYPE", "unknown realtime type code");
    }
    byte[] data = parts.data();
    log.debugRaw("data", "开始解析实时数据, type=" + type + ", dataBytes=" + data.length, data);
    try {
      if (frame.afn().value() == 0x84) {
        if (data.length != 2) return length(frame, 2, data.length);
        List<Measurement> values = Collections.singletonList(measurement("voltage-1", ParameterType.VOLTAGE,
          Sl427DataDecoders.unsigned(data, 0, 2, 2), "V"));
        logMeasurements(type, values, null);
        return success(frame, type, values, null, parts);
      }
      if (data.length < 4) return invalid(frame, "DATA_TRUNCATED", "realtime data requires a 4-byte status tail");
      StatusField status = StatusField.parse(data, data.length - 4);
      int payloadLength = data.length - 4;
      List<Measurement> values = decode(type, data, payloadLength, context, frame);
      logMeasurements(type, values, status);
      return success(frame, type, values, status, parts);
    } catch (ConfigMismatch error) {
      log.warn("data", "点位配置数量不匹配, type=" + type + ", expected=" + error.expected
        + ", actual=" + error.actual);
      return ParseResult.failure(ParseStatus.CONFIG_MISMATCH,
        new ParseIssue("POINT_COUNT_MISMATCH", "context.points", -1,
          Integer.toString(error.expected), Integer.toString(error.actual), "configured point count does not match frame"),
        frame.rawFrame());
    } catch (IllegalArgumentException error) {
      log.warn("data", "实时数据解析失败, type=" + type + ", message=" + error.getMessage());
      return invalid(frame, "INVALID_DATA_ITEM", error.getMessage());
    }
  }

  private List<Measurement> decode(ParameterType type, byte[] data, int length,
                                   ProtocolContext context, Sl427Frame frame) {
    log.debug("data", "分派数据项解码器, type=" + type + ", payloadBytes=" + length);
    List<Measurement> out = new ArrayList<>();
    switch (type) {
      case RAINFALL:
        requireLength(length, 3); add(out, points(context, type, 1, frame), type, data, 0, 3, 1, "mm", false); break;
      case WATER_LEVEL:
        decodeRepeated(out, context, type, data, length, 4, 3, "m", true, frame); break;
      case FLOW_TOTAL:
        decodeFlow(out, context, data, length, frame); break;
      case VELOCITY:
        decodeRepeated(out, context, type, data, length, 3, 3, "m/s", true, frame); break;
      case GATE_POSITION:
        decodeRepeated(out, context, type, data, length, 3, 2, "m", false, frame); break;
      case POWER:
        decodeRepeated(out, context, type, data, length, 3, 0, "kW", false, frame); break;
      case METEOROLOGY:
        requireLength(length, 8);
        out.add(measurement("meteorology-pressure", type, Sl427DataDecoders.unsigned(data, 0, 3, 0), "10Pa"));
        byte[] wind = Arrays.copyOfRange(data, 3, 6);
        int direction = (wind[2] >>> 4) & 0x0F;
        boolean invalidWind = io.github.kukpt.sl427.buffer.BcdCodec.isAllA(wind, 0, wind.length);
        if (!invalidWind && direction > 8) throw new IllegalArgumentException("wind direction must be in 0..8");
        Value windValue;
        if (invalidWind) {
          windValue = Sl427DataDecoders.unsigned(wind, 0, 3, 2);
        } else {
          wind[2] = (byte) (wind[2] & 0x0F);
          windValue = Sl427DataDecoders.unsigned(wind, 0, 3, 2);
        }
        out.add(measurement("meteorology-wind", type, windValue, "m/s"));
        out.add(new Measurement("meteorology-wind-direction", type,
          invalidWind ? null : Integer.valueOf(direction), null,
          invalidWind ? DataQuality.INVALID : DataQuality.GOOD, new byte[] {data[5]}));
        out.add(measurement("meteorology-temperature", type, Sl427DataDecoders.signed(data, 6, 2, 1), "℃"));
        break;
      case ELECTRICITY:
        decodeRepeated(out, context, type, data, length, 4, 2, "kWh", false, frame); break;
      case WATER_TEMPERATURE:
        decodeRepeated(out, context, type, data, length, 2, 1, "℃", false, frame); break;
      case WATER_QUALITY:
        decodeWaterQuality(out, context, data, length, frame); break;
      case SOIL_MOISTURE:
        decodeSoil(out, context, data, length, frame); break;
      case EVAPORATION:
        decodeRepeated(out, context, type, data, length, 3, 1, "mm", false, frame); break;
      case STATUS:
        requireLength(length, 0); break;
      case STATISTICAL_RAINFALL:
        requireLength(length, 4); out.add(measurement("statistical-rainfall-1", type,
          Sl427DataDecoders.unsigned(data, 0, 4, 1), "mm")); break;
      case VOLTAGE:
        requireLength(length, 2); out.add(measurement("voltage-1", type,
          Sl427DataDecoders.unsigned(data, 0, 2, 2), "V")); break;
      case PRESSURE:
        decodeRepeated(out, context, type, data, length, 4, 2, "kPa", false, frame); break;
      case COMPOSITE:
        decodeComposite(out, context, data, length, frame); break;
      default:
        throw new IllegalArgumentException("unsupported parameter type " + type);
    }
    return out;
  }

  private static void logMeasurements(ParameterType type, List<Measurement> values, StatusField status) {
    log.debug("data", "实时数据解析完成, type=" + type + ", measurementCount=" + values.size()
      + ", status=" + status);
    if (log.isDebugEnabled()) {
      for (int i = 0; i < values.size(); i++) {
        log.debug("measurement", "数据项[" + i + "]=" + values.get(i));
      }
    }
  }

  private void decodeRepeated(List<Measurement> out, ProtocolContext context, ParameterType type,
                              byte[] data, int length, int width, int scale, String unit,
                              boolean signed, Sl427Frame frame) {
    if (length % width != 0) throw new IllegalArgumentException("data length is not divisible by item width");
    int count = length / width;
    List<? extends ProtocolPoint> points = points(context, type, count, frame);
    for (int i = 0; i < count; i++) add(out, points, type, data, i * width, width, scale, unit, signed);
  }

  private void decodeFlow(List<Measurement> out, ProtocolContext context, byte[] data,
                          int length, Sl427Frame frame) {
    if (length % 10 != 0) throw new IllegalArgumentException("flow data length must be 10*N");
    int count = length / 10;
    List<? extends ProtocolPoint> points = points(context, ParameterType.FLOW_TOTAL, count, frame);
    for (int i = 0; i < count; i++) {
      String id = pointId(points, i, "flow-total");
      out.add(measurement(id + ".flow", ParameterType.FLOW, Sl427DataDecoders.flow(data, i * 10),
        Sl427DataDecoders.flowUnit(data, i * 10)));
      out.add(measurement(id + ".total-water", ParameterType.TOTAL_WATER,
        Sl427DataDecoders.unsigned(data, i * 10 + 5, 5, 0), "m3"));
    }
  }

  private void decodeWaterQuality(List<Measurement> out, ProtocolContext context, byte[] data,
                                  int length, Sl427Frame frame) {
    if (length < 5 || (length - 5) % 4 != 0) throw new IllegalArgumentException("water quality layout must be bitmap(5)+4*N");
    int expected = 0;
    for (int i = 0; i < 5; i++) expected += Integer.bitCount(data[i] & 0xFF);
    int actual = (length - 5) / 4;
    if (expected != actual) throw new ConfigMismatch(expected, actual);
    List<? extends ProtocolPoint> points = points(context, ParameterType.WATER_QUALITY, actual, frame);
    int valueIndex = 0;
    for (int bit = 0; bit < 40; bit++) {
      if ((data[bit / 8] & (1 << (bit % 8))) == 0) continue;
      String id = points.isEmpty() ? "water-quality-D" + bit : points.get(valueIndex).id();
      out.add(measurement(id, ParameterType.WATER_QUALITY,
        Sl427DataDecoders.unsigned(data, 5 + valueIndex * 4, 4, WATER_QUALITY_SCALES[bit]), null));
      valueIndex++;
    }
  }

  private void decodeSoil(List<Measurement> out, ProtocolContext context, byte[] data,
                          int length, Sl427Frame frame) {
    if (length % 4 != 0) throw new IllegalArgumentException("soil data length must be 4*N");
    int count = length / 4;
    List<? extends ProtocolPoint> points = points(context, ParameterType.SOIL_MOISTURE, count, frame);
    for (int i = 0; i < count; i++) {
      String id = pointId(points, i, "soil");
      out.add(measurement(id + ".moisture", ParameterType.SOIL_MOISTURE,
        Sl427DataDecoders.unsigned(data, i * 4, 2, 1), null));
      out.add(measurement(id + ".depth", ParameterType.SOIL_MOISTURE,
        Sl427DataDecoders.unsigned(data, i * 4 + 2, 2, 0), "cm"));
    }
  }

  /** B0 type 14: bitmap bits D0..D7, followed by each selected item in that order. */
  private void decodeComposite(List<Measurement> out, ProtocolContext context, byte[] data,
                               int length, Sl427Frame frame) {
    if (length < 1) throw new IllegalArgumentException("composite data requires a parameter bitmap");
    int bitmap = data[0] & 0xFF;
    int cursor = 1;
    for (int bit = 0; bit < 8; bit++) {
      if ((bitmap & (1 << bit)) == 0) continue;
      switch (bit) {
        case 0: {
          if (cursor + 5 > length) throw new IllegalArgumentException("composite water quality bitmap is truncated");
          int count = 0;
          for (int i = 0; i < 5; i++) count += Integer.bitCount(data[cursor + i] & 0xFF);
          int width = 5 + count * 4;
          requireAvailable(cursor, width, length);
          byte[] value = Arrays.copyOfRange(data, cursor, cursor + width);
          decodeWaterQuality(out, context, value, value.length, frame);
          cursor += width;
          break;
        }
        case 1: {
          requireAvailable(cursor, 4, length);
          List<? extends ProtocolPoint> points = points(context, ParameterType.SOIL_MOISTURE, 1, frame);
          String id = pointId(points, 0, "soil");
          out.add(measurement(id + ".moisture", ParameterType.SOIL_MOISTURE,
            Sl427DataDecoders.unsigned(data, cursor, 2, 1), null));
          out.add(measurement(id + ".depth", ParameterType.SOIL_MOISTURE,
            Sl427DataDecoders.unsigned(data, cursor + 2, 2, 0), "cm"));
          cursor += 4;
          break;
        }
        case 2:
          requireAvailable(cursor, 3, length);
          add(out, points(context, ParameterType.POWER, 1, frame), ParameterType.POWER,
            Arrays.copyOfRange(data, cursor, cursor + 3), 0, 3, 0, "kW", false); cursor += 3; break;
        case 3: {
          requireAvailable(cursor, 8, length);
          byte[] meteorology = new byte[12];
          System.arraycopy(data, cursor, meteorology, 0, 8);
          // Reuse the standalone decoder by supplying a disposable status tail.
          decode(ParameterType.METEOROLOGY, meteorology, 8, context, frame).forEach(out::add);
          cursor += 8;
          break;
        }
        case 4:
          requireAvailable(cursor, 3, length);
          add(out, points(context, ParameterType.GATE_POSITION, 1, frame), ParameterType.GATE_POSITION,
            Arrays.copyOfRange(data, cursor, cursor + 3), 0, 3, 2, "m", false); cursor += 3; break;
        case 5: {
          requireAvailable(cursor, 10, length);
          List<? extends ProtocolPoint> points = points(context, ParameterType.FLOW_TOTAL, 1, frame);
          String id = pointId(points, 0, "flow-total");
          out.add(measurement(id + ".flow", ParameterType.FLOW, Sl427DataDecoders.flow(data, cursor),
            Sl427DataDecoders.flowUnit(data, cursor)));
          out.add(measurement(id + ".total-water", ParameterType.TOTAL_WATER,
            Sl427DataDecoders.unsigned(data, cursor + 5, 5, 0), "m3"));
          cursor += 10;
          break;
        }
        case 6:
          requireAvailable(cursor, 4, length);
          add(out, points(context, ParameterType.WATER_LEVEL, 1, frame), ParameterType.WATER_LEVEL,
            Arrays.copyOfRange(data, cursor, cursor + 4), 0, 4, 3, "m", true); cursor += 4; break;
        case 7:
          requireAvailable(cursor, 3, length);
          add(out, points(context, ParameterType.RAINFALL, 1, frame), ParameterType.RAINFALL,
            Arrays.copyOfRange(data, cursor, cursor + 3), 0, 3, 1, "mm", false); cursor += 3; break;
        default: throw new AssertionError();
      }
    }
    if (cursor != length) throw new IllegalArgumentException("composite data contains trailing bytes");
  }

  private static void add(List<Measurement> out, List<? extends ProtocolPoint> points, ParameterType type,
                          byte[] data, int offset, int width, int scale, String unit, boolean signed) {
    int index = offset / width;
    Value value = signed ? Sl427DataDecoders.signed(data, offset, width, scale)
      : Sl427DataDecoders.unsigned(data, offset, width, scale);
    out.add(measurement(pointId(points, index, type.name().toLowerCase()), type, value, unit));
  }

  private static Measurement measurement(String id, ParameterType type, Value value, String unit) {
    return new Measurement(id, type, value.value(), unit, value.quality(), value.raw());
  }

  private static List<? extends ProtocolPoint> points(ProtocolContext context, ParameterType type,
                                                       int actual, Sl427Frame frame) {
    List<? extends ProtocolPoint> configured = context.points(type);
    if (!configured.isEmpty() && configured.size() != actual) throw new ConfigMismatch(configured.size(), actual);
    return configured;
  }

  private static String pointId(List<? extends ProtocolPoint> points, int index, String prefix) {
    return points.isEmpty() ? prefix + '-' + (index + 1) : points.get(index).id();
  }

  private static void requireLength(int actual, int expected) {
    if (actual != expected) throw new IllegalArgumentException("expected " + expected + " data bytes but got " + actual);
  }

  private static void requireAvailable(int offset, int width, int length) {
    if (offset + width > length) throw new IllegalArgumentException("composite data item is truncated");
  }

  private static ParseResult<ProtocolMessage> success(Sl427Frame frame, ParameterType type,
      List<Measurement> values, StatusField status, BodyParts parts) {
    return ParseResult.<ProtocolMessage>success(
      new RealtimeDataMessage(frame, type, values, status, parts.timeTag()), frame.rawFrame());
  }

  private static ParseResult<ProtocolMessage> length(Sl427Frame frame, int expected, int actual) {
    return ParseResult.failure(ParseStatus.INVALID_DATA,
      new ParseIssue("DATA_LENGTH", "data", 0, Integer.toString(expected), Integer.toString(actual), "unexpected data length"),
      frame.rawFrame());
  }

  private static ParseResult<ProtocolMessage> invalid(Sl427Frame frame, String code, String message) {
    return ParseResult.failure(ParseStatus.INVALID_DATA, ParseIssue.of(code, "data", 0, message), frame.rawFrame());
  }

  private static final class ConfigMismatch extends IllegalArgumentException {
    private final int expected;
    private final int actual;
    private ConfigMismatch(int expected, int actual) { this.expected = expected; this.actual = actual; }
  }
}
