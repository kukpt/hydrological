package io.github.kukpt.sl427.buffer;

import java.math.BigDecimal;

public final class BcdCodec {

  private BcdCodec() {
  }

  public static boolean isAllA(byte[] data, int offset, int length) {
    for (int i = offset; i < offset + length; i++) {
      if ((data[i] & 0xFF) != 0xAA) {
        return false;
      }
    }
    return length > 0;
  }

  public static BigDecimal decodeLittleEndian(byte[] data, int offset, int length, int scale) {
    checkRange(data, offset, length);
    StringBuilder digits = new StringBuilder(length * 2);
    for (int i = offset + length - 1; i >= offset; i--) {
      appendDigit(digits, (data[i] >>> 4) & 0x0F);
      appendDigit(digits, data[i] & 0x0F);
    }
    return new BigDecimal(digits.toString()).movePointLeft(scale);
  }

  public static BigDecimal decodeSignedHighNibble(byte[] data, int offset, int length, int scale) {
    checkRange(data, offset, length);
    int last = data[offset + length - 1] & 0xFF;
    int sign = (last >>> 4) & 0x0F;
    if (sign != 0x0 && sign != 0xF) {
      throw new IllegalArgumentException("invalid BCD sign nibble: " + sign);
    }
    StringBuilder digits = new StringBuilder(length * 2 - 1);
    appendDigit(digits, last & 0x0F);
    for (int i = offset + length - 2; i >= offset; i--) {
      appendDigit(digits, (data[i] >>> 4) & 0x0F);
      appendDigit(digits, data[i] & 0x0F);
    }
    BigDecimal value = new BigDecimal(digits.toString()).movePointLeft(scale);
    return sign == 0xF ? value.negate() : value;
  }

  public static int digit(int value) {
    if (value < 0 || value > 9) {
      throw new IllegalArgumentException("invalid BCD digit: " + value);
    }
    return value;
  }

  private static void appendDigit(StringBuilder builder, int digit) {
    builder.append((char) ('0' + digit(digit)));
  }

  private static void checkRange(byte[] data, int offset, int length) {
    if (data == null) {
      throw new IllegalArgumentException("data must not be null");
    }
    if (length <= 0 || offset < 0 || offset + length > data.length) {
      throw new IndexOutOfBoundsException("invalid BCD range");
    }
  }
}
