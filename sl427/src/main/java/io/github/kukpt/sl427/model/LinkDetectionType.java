package io.github.kukpt.sl427.model;

public enum LinkDetectionType {
  LOGIN(0xF0),
  LOGOUT(0xF1),
  KEEP_ALIVE(0xF2);

  private final int code;

  LinkDetectionType(int code) {
    this.code = code;
  }

  public int code() { return code; }

  public static LinkDetectionType from(int code) {
    for (LinkDetectionType value : values()) {
      if (value.code == code) {
        return value;
      }
    }
    throw new IllegalArgumentException("unsupported link detection value: " + code);
  }
}
