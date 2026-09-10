package io.github.kukpt.sl427.model;

import io.github.kukpt.sl427.buffer.Hex;

public final class Measurement {

  private final String pointId;
  private final ParameterType type;
  private final Object value;
  private final String unit;
  private final DataQuality quality;
  private final byte[] raw;

  public Measurement(String pointId, ParameterType type, Object value, String unit,
                     DataQuality quality, byte[] raw) {
    this.pointId = pointId;
    this.type = type;
    this.value = value;
    this.unit = unit;
    this.quality = quality;
    this.raw = raw.clone();
  }

  public String pointId() { return pointId; }
  public ParameterType type() { return type; }
  public Object value() { return value; }
  public String unit() { return unit; }
  public DataQuality quality() { return quality; }
  public byte[] raw() { return raw.clone(); }

  @Override
  public String toString() {
    return "Measurement{" +
      "pointId='" + pointId + '\'' +
      ", type=" + type +
      ", value=" + value +
      ", unit='" + unit + '\'' +
      ", quality=" + quality +
      ", raw=" + Hex.encode(raw) +
      '}';
  }
}
