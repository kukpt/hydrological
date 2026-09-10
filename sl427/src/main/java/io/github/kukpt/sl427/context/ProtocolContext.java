package io.github.kukpt.sl427.context;

import io.github.kukpt.sl427.frame.AddressMode;
import io.github.kukpt.sl427.model.ParameterType;

import java.util.List;
import java.util.Optional;

public interface ProtocolContext {
  String protocolVersion();
  AddressMode addressMode();
  List<? extends ProtocolPoint> points(ParameterType type);
  Optional<String> property(String key);
}
