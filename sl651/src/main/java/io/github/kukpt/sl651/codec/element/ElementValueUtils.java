package io.github.kukpt.sl651.codec.element;

import io.github.kukpt.sl651.codec.TimeStep;
import io.github.kukpt.sl651.utils.Sl651CodecUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;

import java.nio.charset.StandardCharsets;

public final class ElementValueUtils {

  private ElementValueUtils() {
  }

  public static double doubleValue(byte[] rawValue, ElementId elementId) {
    return doubleValue(rawValue, elementId.numberPoint());
  }

  public static double doubleValue(byte[] rawValue, int numberPoint) {
    ByteBuf byteBuf = Unpooled.wrappedBuffer(rawValue);
    return Sl651CodecUtils.readBcdNumber(
      byteBuf, rawValue.length, numberPoint);
  }

  public static String stringValue(byte[] rawValue) {
    return ByteBufUtil.hexDump(rawValue);
  }

  public static String asciiValue(byte[] rawValue) {
    return new String(rawValue, StandardCharsets.US_ASCII);
  }

  public static int intValue(byte[] rawValue) {
    requireRawLength(rawValue, 4, "INT");
    return Unpooled.wrappedBuffer(rawValue).readInt();
  }

  public static long longValue(byte[] rawValue) {
    requireRawLength(rawValue, 8, "LONG");
    return Unpooled.wrappedBuffer(rawValue).readLong();
  }

  public static int status8Value(byte[] rawValue) {
    requireRawLength(rawValue, 1, "STATUS_8");
    return rawValue[0] & 0xFF;
  }

  public static int status16Value(byte[] rawValue) {
    requireRawLength(rawValue, 2, "STATUS_16");
    return Unpooled.wrappedBuffer(rawValue).readUnsignedShort();
  }

  public static long status32Value(byte[] rawValue) {
    requireRawLength(rawValue, 4, "STATUS_32");
    return Unpooled.wrappedBuffer(rawValue).readUnsignedInt();
  }

  public static double[] doubleArrayValue(byte[] rawValue, ElementId elementId) {
    ByteBuf byteBuf = Unpooled.wrappedBuffer(rawValue);
    if (elementId.id() == 0xF4) {
      double[] values = new double[rawValue.length];
      for (int i = 0; i < values.length; i++) {
        values[i] = (double) byteBuf.readUnsignedByte() / Sl651CodecUtils.ptn(1);
      }
      return values;
    }
    if (elementId.id() >= 0xF5 && elementId.id() <= 0xFC) {
      if ((rawValue.length & 1) != 0) {
        throw new IllegalStateException(
          "Water-level array must contain an even number of bytes");
      }
      double[] values = new double[rawValue.length / 2];
      for (int i = 0; i < values.length; i++) {
        values[i] = (double) byteBuf.readUnsignedShort() / Sl651CodecUtils.ptn(2);
      }
      return values;
    }
    throw new IllegalStateException(
      "Unsupported double array element: 0x" + elementId.toHex());
  }

  public static TimeStep timeStepValue(byte[] rawValue) {
    requireRawLength(rawValue, 3, "TIME_STEP");
    return TimeStep.createTimeStep(Unpooled.wrappedBuffer(rawValue));
  }

  private static void requireRawLength(
    byte[] rawValue, int expectedLength, String targetType) {
    if (rawValue.length != expectedLength) {
      throw new IllegalStateException(
        targetType + " value must contain " + expectedLength
          + " bytes, but was " + rawValue.length);
    }
  }
}
