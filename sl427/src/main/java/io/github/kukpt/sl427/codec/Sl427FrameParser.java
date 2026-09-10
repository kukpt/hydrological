package io.github.kukpt.sl427.codec;

import io.github.kukpt.hydrological.common.ProtocolDecodeLogger;
import io.github.kukpt.hydrological.common.ProtocolType;
import io.github.kukpt.sl427.checksum.Sl427Crc8;
import io.github.kukpt.sl427.frame.Afn;
import io.github.kukpt.sl427.frame.AddressMode;
import io.github.kukpt.sl427.frame.ControlField;
import io.github.kukpt.sl427.frame.Sl427Frame;
import io.github.kukpt.sl427.frame.StationAddress;
import io.github.kukpt.sl427.result.ParseIssue;
import io.github.kukpt.sl427.result.ParseResult;
import io.github.kukpt.sl427.result.ParseStatus;

import java.util.Arrays;
import java.util.Objects;

public final class Sl427FrameParser {

  private static final ProtocolDecodeLogger log =
    ProtocolDecodeLogger.getLogger(Sl427FrameParser.class, ProtocolType.SL427);

  private static final int START = 0x68;
  private static final int END = 0x16;
  private final AddressMode addressMode;

  public Sl427FrameParser() {
    this(AddressMode.ADMINISTRATIVE);
  }

  public Sl427FrameParser(AddressMode addressMode) {
    this.addressMode = Objects.requireNonNull(addressMode, "addressMode");
  }

  public ParseResult<Sl427Frame> parse(byte[] raw) {
    log.debugRaw("frame", "开始校验并解析完整帧, bytes=" + lengthOf(raw), raw);
    if (raw == null || raw.length < 12) {
      return invalid("FRAME_TOO_SHORT", "frame", -1, "at least 12 bytes", lengthOf(raw), raw);
    }
    int declaredLength = raw[1] & 0xFF;
    int expectedLength = declaredLength + 5;
    if ((raw[0] & 0xFF) != START || (raw[2] & 0xFF) != START) {
      return invalid("INVALID_START", "start", 0, "68 L 68", hexAt(raw, 0) + " .. " + hexAt(raw, 2), raw);
    }
    if (raw.length != expectedLength) {
      return invalid("LENGTH_MISMATCH", "length", 1,
        Integer.toString(expectedLength), Integer.toString(raw.length), raw);
    }
    if ((raw[raw.length - 1] & 0xFF) != END) {
      return invalid("INVALID_END", "end", raw.length - 1, "16", hexAt(raw, raw.length - 1), raw);
    }
    int expectedCrc = raw[raw.length - 2] & 0xFF;
    int actualCrc = Sl427Crc8.calculate(raw, 3, declaredLength);
    if (expectedCrc != actualCrc) {
      return invalid("CRC_MISMATCH", "checksum", raw.length - 2,
        String.format("%02X", expectedCrc), String.format("%02X", actualCrc), raw);
    }

    try {
      int position = 3;
      ControlField control = ControlField.from(raw[position++] & 0xFF);
      Integer splitCountdown = null;
      if (control.split()) {
        splitCountdown = raw[position++] & 0xFF;
        if (splitCountdown < 1) {
          return invalid("INVALID_DIVS", "control.divs", position - 1, "1..255", "0", raw);
        }
      }
      int userDataEnd = 3 + declaredLength;
      if (position + 6 > userDataEnd) {
        return invalid("HEADER_TOO_SHORT", "userData", position, "address + AFN", "truncated", raw);
      }
      StationAddress address = StationAddress.parse(
        Arrays.copyOfRange(raw, position, position + 5), addressMode);
      position += 5;

      int firstAfn = raw[position++] & 0xFF;
      Afn afn;
      if (firstAfn == 0xFF) {
        if (position >= userDataEnd) {
          return invalid("CUSTOM_AFN_TRUNCATED", "afn", position - 1, "FF 01..FE", "FF", raw);
        }
        afn = Afn.custom(raw[position++] & 0xFF);
      } else {
        afn = Afn.standard(firstAfn);
      }
      byte[] body = Arrays.copyOfRange(raw, position, userDataEnd);
      log.debug("header", "帧头解析完成, address=" + address + ", control=" + control
        + ", afn=" + afn + ", divs=" + splitCountdown + ", bodyBytes=" + body.length
        + ", checksum=0x" + String.format("%02X", expectedCrc));
      return ParseResult.success(
        new Sl427Frame(declaredLength, control, splitCountdown, address, afn, body, expectedCrc, raw), raw);
    } catch (IllegalArgumentException error) {
      log.warn("header", "帧头解析失败: " + error.getMessage());
      return ParseResult.failure(ParseStatus.INVALID_FRAME,
        ParseIssue.of("INVALID_HEADER", "header", 3, error.getMessage()), raw);
    }
  }

  private static ParseResult<Sl427Frame> invalid(String code, String field, int offset,
                                                  String expected, String actual, byte[] raw) {
    log.warn("validation", "帧校验失败, code=" + code + ", field=" + field
      + ", offset=" + offset + ", expected=" + expected + ", actual=" + actual
      + ", raw=" + ProtocolDecodeLogger.hex(raw));
    return ParseResult.failure(ParseStatus.INVALID_FRAME,
      new ParseIssue(code, field, offset, expected, actual, code), raw);
  }

  private static String lengthOf(byte[] raw) {
    return raw == null ? "null" : Integer.toString(raw.length);
  }

  private static String hexAt(byte[] raw, int index) {
    return index < 0 || index >= raw.length ? "missing" : String.format("%02X", raw[index] & 0xFF);
  }
}
