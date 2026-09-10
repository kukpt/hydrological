package io.github.kukpt.sl427.model;

/** Terminal work mode carried by the one-byte data field of an S2 confirmation. */
public enum TerminalWorkMode {
  COMPATIBLE(0x00),
  SELF_REPORT(0x01),
  QUERY_RESPONSE(0x02),
  MAINTENANCE(0x03);

  private final int code;

  TerminalWorkMode(int code) {
    this.code = code;
  }

  public int code() { return code; }
}
