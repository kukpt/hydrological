package io.github.kukpt.sl427.model;

public final class StatusField {

  private final int alarmBits;
  private final int terminalBits;

  public StatusField(int alarmBits, int terminalBits) {
    this.alarmBits = alarmBits & 0xFFFF;
    this.terminalBits = terminalBits & 0xFFFF;
  }

  public static StatusField parse(byte[] data, int offset) {
    if (offset < 0 || offset + 4 > data.length) {
      throw new IllegalArgumentException("status field requires 4 bytes");
    }
    int alarm = (data[offset] & 0xFF) | ((data[offset + 1] & 0xFF) << 8);
    int terminal = (data[offset + 2] & 0xFF) | ((data[offset + 3] & 0xFF) << 8);
    return new StatusField(alarm, terminal);
  }

  public int alarmBits() { return alarmBits; }
  public int terminalBits() { return terminalBits; }
  public boolean alarmBit(int bit) { return bit >= 0 && bit < 16 && (alarmBits & (1 << bit)) != 0; }
  public boolean terminalBit(int bit) { return bit >= 0 && bit < 16 && (terminalBits & (1 << bit)) != 0; }
}
