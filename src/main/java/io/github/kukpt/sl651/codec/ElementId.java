package io.github.kukpt.sl651.codec;

public class ElementId {

  private final int id;

  private final int readLength;

  private final int numberPoint;

  private final int consumed;

  public int id() {
    return id;
  }

  int readLength() {
    return readLength;
  }

  int numberPoint() {
    return numberPoint;
  }

  int consumed() {
    return consumed;
  }

  public ElementId (int id, int readLength, int numberPoint, int consumed) {
    this.id = id;
    this.readLength = readLength;
    this.numberPoint = numberPoint;
    this.consumed = consumed;
  }

  public String toHex() {
    return Integer.toHexString(id);
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("ElementId{");
    sb.append("id=0x").append(Integer.toHexString(id));
    sb.append('}');
    return sb.toString();
  }
}
