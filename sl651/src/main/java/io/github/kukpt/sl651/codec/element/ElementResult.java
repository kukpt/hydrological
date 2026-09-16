package io.github.kukpt.sl651.codec.element;


import io.github.kukpt.sl651.codec.TimeStep;

import java.util.Arrays;

public class ElementResult {

  private final ElementValueType valueType;

  private final ElementId elementId;

  /**
   * The bytes exactly as they appeared in the message body. Typed values are
   * derived from these bytes on demand so the original value is never lost.
   */
  private final byte[] rawValue;

  private final int numberOfBytesConsumed;

  int getNumberOfBytesConsumed() {
    return this.numberOfBytesConsumed;
  }

  public ElementId elementId() {
    return this.elementId;
  }

  public ElementValueType valueType() {
    return this.valueType;
  }

  /**
   * Returns a defensive copy of the element value as received on the wire.
   */
  public byte[] rawValue() {
    return this.rawValue.clone();
  }

  public long statusValue() {
    requireValueType(ElementValueType.STATUS);
    return this.status32Value();
  }

  public double doubleValue() {
    requireValueType(ElementValueType.DOUBLE);
    return ElementValueUtils.doubleValue(this.rawValue, this.elementId);
  }

  public String stringValue() {
    requireValueType(ElementValueType.STRING);
    return ElementValueUtils.stringValue(this.rawValue);
  }

  public double[] doubleArrayValue() {
    requireValueType(ElementValueType.DOUBLE_ARRAY);
    return ElementValueUtils.doubleArrayValue(this.rawValue, this.elementId);
  }

  public byte[] byteArrayValue() {
    requireValueType(ElementValueType.BYTE_ARRAY);
    return this.rawValue();
  }

  public TimeStep timeStepValue() {
    requireValueType(ElementValueType.TIME_STEP);
    return ElementValueUtils.timeStepValue(this.rawValue);
  }

  public String asciiValue() {
    return ElementValueUtils.asciiValue(this.rawValue);
  }

  public int intValue() {
    return ElementValueUtils.intValue(this.rawValue);
  }

  public long longValue() {
    return ElementValueUtils.longValue(this.rawValue);
  }

  public int status8Value() {
    return ElementValueUtils.status8Value(this.rawValue);
  }

  public int status16Value() {
    return ElementValueUtils.status16Value(this.rawValue);
  }

  public long status32Value() {
    return ElementValueUtils.status32Value(this.rawValue);
  }

  private void requireValueType(ElementValueType expectedValueType) {
    if (this.valueType != expectedValueType) {
      throw new IllegalStateException("Unsupported value type: " + this.valueType);
    }
  }

  public String value() {
    switch (this.valueType) {
      case STATUS:
        return Long.toBinaryString(this.statusValue());
      case DOUBLE_ARRAY:
        return Arrays.toString(this.doubleArrayValue());
      case BYTE_ARRAY:
        return Arrays.toString(this.byteArrayValue());
      case STRING:
        return this.stringValue();
      case DOUBLE:
        return Double.toString(this.doubleValue());
      case TIME_STEP:
        return this.timeStepValue().toString();
      default:
        return Arrays.toString(this.rawValue);
    }
  }

  ElementResult(ElementValueType valueType, byte[] rawValue, ElementId elementId,
                int numberOfBytesConsumed) {
    this.valueType = valueType;
    this.rawValue = rawValue.clone();
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
