package io.github.kukpt.sl427.checksum;

public final class Sl427Crc8 {

  public static final int POLYNOMIAL = 0xE5;

  private Sl427Crc8() {
  }

  public static int calculate(byte[] data) {
    return calculate(data, 0, data.length);
  }

  public static int calculate(byte[] data, int offset, int length) {
    if (data == null) {
      throw new IllegalArgumentException("data must not be null");
    }
    if (offset < 0 || length < 0 || offset + length > data.length) {
      throw new IndexOutOfBoundsException("invalid CRC range");
    }
    int crc = 0;
    for (int i = offset; i < offset + length; i++) {
      crc ^= data[i] & 0xFF;
      for (int bit = 0; bit < 8; bit++) {
        crc = (crc & 0x80) != 0 ? ((crc << 1) ^ POLYNOMIAL) & 0xFF : (crc << 1) & 0xFF;
      }
    }
    return crc;
  }
}
