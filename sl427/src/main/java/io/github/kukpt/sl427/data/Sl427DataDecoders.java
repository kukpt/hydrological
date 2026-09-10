package io.github.kukpt.sl427.data;

import io.github.kukpt.sl427.buffer.BcdCodec;
import io.github.kukpt.sl427.model.DataQuality;

import java.math.BigDecimal;
import java.util.Arrays;

/** Strict primitives used by the AFN parsers. */
public final class Sl427DataDecoders {
  private Sl427DataDecoders() { }

  public static Value unsigned(byte[] data, int offset, int length, int scale) {
    byte[] raw = slice(data, offset, length);
    if (BcdCodec.isAllA(raw, 0, raw.length)) return new Value(null, DataQuality.INVALID, raw);
    return new Value(BcdCodec.decodeLittleEndian(raw, 0, raw.length, scale), DataQuality.GOOD, raw);
  }

  public static Value signed(byte[] data, int offset, int length, int scale) {
    byte[] raw = slice(data, offset, length);
    if (BcdCodec.isAllA(raw, 0, raw.length)) return new Value(null, DataQuality.INVALID, raw);
    return new Value(BcdCodec.decodeSignedHighNibble(raw, 0, raw.length, scale), DataQuality.GOOD, raw);
  }

  public static Value flow(byte[] data, int offset) {
    byte[] raw = slice(data, offset, 5);
    if (BcdCodec.isAllA(raw, 0, raw.length)) return new Value(null, DataQuality.INVALID, raw);
    int flags = (raw[4] >>> 6) & 0x03;
    if (flags != 0 && flags != 3) throw new IllegalArgumentException("invalid flow sign bits");
    byte[] numeric = raw.clone();
    numeric[4] = (byte) (numeric[4] & 0x0F);
    BigDecimal value = BcdCodec.decodeLittleEndian(numeric, 0, numeric.length, 3);
    return new Value(flags == 3 ? value.negate() : value, DataQuality.GOOD, raw);
  }

  public static String flowUnit(byte[] data, int offset) {
    byte[] raw = slice(data, offset, 5);
    if (BcdCodec.isAllA(raw, 0, raw.length)) return "m3/s";
    int unitBits = (raw[4] >>> 4) & 0x03;
    if (unitBits == 0) return "m3/s";
    if (unitBits == 3) return "m3/h";
    throw new IllegalArgumentException("invalid flow unit bits");
  }

  private static byte[] slice(byte[] data, int offset, int length) {
    if (data == null || offset < 0 || length <= 0 || offset + length > data.length) {
      throw new IllegalArgumentException("data item is truncated");
    }
    return Arrays.copyOfRange(data, offset, offset + length);
  }

  public static final class Value {
    private final BigDecimal value;
    private final DataQuality quality;
    private final byte[] raw;
    private Value(BigDecimal value, DataQuality quality, byte[] raw) {
      this.value = value;
      this.quality = quality;
      this.raw = raw;
    }
    public BigDecimal value() { return value; }
    public DataQuality quality() { return quality; }
    public byte[] raw() { return raw.clone(); }
  }
}
