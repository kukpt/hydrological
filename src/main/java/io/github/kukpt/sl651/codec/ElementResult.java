package io.github.kukpt.sl651.codec;

public class ElementResult {

  private final ElementId elementId;

  private final Object value;

  private final int numberOfBytesConsumed;

  int getNumberOfBytesConsumed() {
    return this.numberOfBytesConsumed;
  }

  public ElementId elementId() {
    return this.elementId;
  }


  public Object value() {
    return this.value;
  }

  ElementResult(Object value, ElementId elementId, int numberOfBytesConsumed) {
    this.value = value;
    this.elementId = elementId;
    this.numberOfBytesConsumed = numberOfBytesConsumed;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("ElementResult{");
    sb.append("elementId=").append(elementId);
    sb.append(", value=").append(value);
    sb.append('}');
    return sb.toString();
  }
}
