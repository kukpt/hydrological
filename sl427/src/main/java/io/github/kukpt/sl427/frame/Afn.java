package io.github.kukpt.sl427.frame;

public final class Afn {

  private final int value;
  private final int byteLength;

  private Afn(int value, int byteLength) {
    this.value = value;
    this.byteLength = byteLength;
  }

  public static Afn standard(int value) {
    if (value < 0 || value > 0xFE) {
      throw new IllegalArgumentException("standard AFN must be in 00H..FEH");
    }
    return new Afn(value, 1);
  }

  public static Afn custom(int extension) {
    if (extension < 1 || extension > 0xFE) {
      throw new IllegalArgumentException("custom AFN extension must be in 01H..FEH");
    }
    return new Afn(0xFF00 | extension, 2);
  }

  public int value() { return value; }
  public int byteLength() { return byteLength; }
  public boolean custom() { return byteLength == 2; }

  @Override
  public boolean equals(Object other) {
    return other instanceof Afn && ((Afn) other).value == value;
  }

  @Override
  public int hashCode() { return value; }

  @Override
  public String toString() {
    return String.format(custom() ? "%04XH" : "%02XH", value);
  }
}
