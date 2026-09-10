package io.github.kukpt.sl427.context;

import io.github.kukpt.sl427.model.ParameterType;

public interface ProtocolPoint {
  String id();
  ParameterType type();
  int channelNo();
  String unit();
}
