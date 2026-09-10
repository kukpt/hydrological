package io.github.kukpt.sl427.context;

import io.github.kukpt.sl427.model.ParameterType;

public final class SimpleProtocolPoint implements ProtocolPoint {

  private final String id;
  private final ParameterType type;
  private final int channelNo;
  private final String unit;

  public SimpleProtocolPoint(String id, ParameterType type, int channelNo, String unit) {
    this.id = id;
    this.type = type;
    this.channelNo = channelNo;
    this.unit = unit;
  }

  @Override public String id() { return id; }
  @Override public ParameterType type() { return type; }
  @Override public int channelNo() { return channelNo; }
  @Override public String unit() { return unit; }
}
