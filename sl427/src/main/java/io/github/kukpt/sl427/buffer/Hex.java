package io.github.kukpt.sl427.buffer;

public final class Hex {

  private static final char[] DIGITS = "0123456789ABCDEF".toCharArray();

  private Hex() {
  }

  public static String encode(byte[] bytes) {
    char[] chars = new char[bytes.length * 2];
    for (int i = 0; i < bytes.length; i++) {
      int value = bytes[i] & 0xFF;
      chars[i * 2] = DIGITS[value >>> 4];
      chars[i * 2 + 1] = DIGITS[value & 0x0F];
    }
    return new String(chars);
  }

  public static byte[] decode(String value) {
    String normalized = value.replaceAll("\\s+", "");
    if ((normalized.length() & 1) != 0) {
      throw new IllegalArgumentException("hex string must contain an even number of digits");
    }
    byte[] bytes = new byte[normalized.length() / 2];
    for (int i = 0; i < bytes.length; i++) {
      int high = Character.digit(normalized.charAt(i * 2), 16);
      int low = Character.digit(normalized.charAt(i * 2 + 1), 16);
      if (high < 0 || low < 0) {
        throw new IllegalArgumentException("invalid hex string");
      }
      bytes[i] = (byte) ((high << 4) | low);
    }
    return bytes;
  }
}
