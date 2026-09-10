package io.github.kukpt.sl427.frame;

public final class ControlField {

  private final int raw;

  private ControlField(int raw) {
    this.raw = raw;
  }

  public static ControlField from(int raw) {
    if (raw < 0 || raw > 0xFF) {
      throw new IllegalArgumentException("control field must be an unsigned byte");
    }
    return new ControlField(raw);
  }

  public int raw() { return raw; }
  public Direction direction() { return (raw & 0x80) == 0 ? Direction.DOWNSTREAM : Direction.UPSTREAM; }
  public boolean split() { return (raw & 0x40) != 0; }
  public int frameCount() { return (raw >>> 4) & 0x03; }
  public int typeCode() { return raw & 0x0F; }

  public ControlField withoutSplit() {
    return new ControlField(raw & ~0x40);
  }

  @Override
  public String toString() {
    return "ControlField{" +
      "raw=0x" + Integer.toHexString(raw).toUpperCase() +
      ", direction=" + direction() +
      ", split=" + split() +
      ", frameCount=" + frameCount() +
      ", typeCode=" + typeCode() +
      '}';
  }
}
