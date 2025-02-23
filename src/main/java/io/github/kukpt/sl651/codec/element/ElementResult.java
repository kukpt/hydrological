package io.github.kukpt.sl651.codec.element;


import io.github.kukpt.sl651.codec.TimeStep;

import java.util.Arrays;

public class ElementResult {

  private final ElementValueType valueType;

  private final ElementId elementId;

  private final Object value;

  private final int numberOfBytesConsumed;

  int getNumberOfBytesConsumed() {
    return this.numberOfBytesConsumed;
  }

  public ElementId elementId() {
    return this.elementId;
  }

  public long statusValue() {
    if (this.valueType == ElementValueType.STATUS) {
      return (Long) this.value;
    }
    throw new IllegalStateException("Unsupported value type: " + this.valueType);
  }

  public double doubleValue() {
    if (this.valueType == ElementValueType.DOUBLE) {
      return (Double) this.value;
    }
    throw new IllegalStateException("Unsupported value type: " + this.valueType);
  }

  public String stringValue() {
    if (this.valueType == ElementValueType.STRING) {
      return (String) this.value;
    }
    throw new IllegalStateException("Unsupported value type: " + this.valueType);
  }

  public double[] doubleArrayValue() {
    if (this.valueType == ElementValueType.DOUBLE_ARRAY) {
      return (double[]) this.value;
    }
    throw new IllegalStateException("Unsupported value type: " + this.valueType);
  }

  public TimeStep timeStepValue() {
    if (this.valueType == ElementValueType.TIME_STEP) {
      return (TimeStep) this.value;
    }
    throw new IllegalStateException("Unsupported value type: " + this.valueType);
  }

  public String value() {
    switch (this.valueType) {
      case STATUS:
        return Long.toBinaryString(this.statusValue());
      case DOUBLE_ARRAY:
        return Arrays.toString(this.doubleArrayValue());
      case STRING:
        return this.stringValue();
      case DOUBLE:
        return Double.toString(this.doubleValue());
      case TIME_STEP:
        return this.timeStepValue().toString();
        default:
          return this.value.toString();
    }
  }

  ElementResult(ElementValueType valueType, Object value, ElementId elementId, int numberOfBytesConsumed) {
    this.valueType = valueType;
    this.value = value;
    this.elementId = elementId;
    this.numberOfBytesConsumed = numberOfBytesConsumed;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("{");
    sb.append("elementId=").append(elementId);
    sb.append(", valueType=").append(valueType);
    sb.append(", value=").append(this.value());
    sb.append('}');
    return sb.toString();
  }
}
