package io.github.kukpt.sl427.frame;

import io.github.kukpt.sl427.buffer.BcdCodec;

import java.time.DateTimeException;
import java.time.LocalDateTime;

public final class TimeTag {

  private final LocalDateTime timestamp;
  private final int delayMinutes;

  private TimeTag(LocalDateTime timestamp, int delayMinutes) {
    this.timestamp = timestamp;
    this.delayMinutes = delayMinutes;
  }

  public static TimeTag of(LocalDateTime timestamp, int delayMinutes) {
    if (timestamp == null) throw new IllegalArgumentException("timestamp is required");
    if (timestamp.getYear() < 2000 || timestamp.getYear() > 2099) {
      throw new IllegalArgumentException("time tag year must be in 2000..2099");
    }
    if (delayMinutes < 0 || delayMinutes > 255) {
      throw new IllegalArgumentException("delayMinutes must be in 0..255");
    }
    return new TimeTag(timestamp, delayMinutes);
  }

  public static TimeTag parse(byte[] bytes, int offset) {
    if (bytes == null || offset < 0 || offset + 7 > bytes.length) {
      throw new IllegalArgumentException("time tag requires 7 bytes");
    }
    int second = bcdByte(bytes[offset]);
    int minute = bcdByte(bytes[offset + 1]);
    int hour = bcdByte(bytes[offset + 2]);
    int day = bcdByte(bytes[offset + 3]);
    int month = bcdByte(bytes[offset + 4]);
    int year = 2000 + bcdByte(bytes[offset + 5]);
    try {
      return new TimeTag(LocalDateTime.of(year, month, day, hour, minute, second), bytes[offset + 6] & 0xFF);
    } catch (DateTimeException error) {
      throw new IllegalArgumentException("invalid SL427 time tag", error);
    }
  }

  private static int bcdByte(byte value) {
    return BcdCodec.digit((value >>> 4) & 0x0F) * 10 + BcdCodec.digit(value & 0x0F);
  }

  public LocalDateTime timestamp() { return timestamp; }
  public int delayMinutes() { return delayMinutes; }

  public byte[] encode() {
    return new byte[] {
      bcd(timestamp.getSecond()),
      bcd(timestamp.getMinute()),
      bcd(timestamp.getHour()),
      bcd(timestamp.getDayOfMonth()),
      bcd(timestamp.getMonthValue()),
      bcd(timestamp.getYear() % 100),
      (byte) delayMinutes
    };
  }

  private static byte bcd(int value) {
    return (byte) (((value / 10) << 4) | (value % 10));
  }

  @Override
  public String toString() {
    return "TimeTag{" + "timestamp=" + timestamp + ", delayMinutes=" + delayMinutes + '}';
  }
}
