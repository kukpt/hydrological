package io.github.kukpt.sl427.context;

import io.github.kukpt.sl427.frame.AddressMode;
import io.github.kukpt.sl427.model.ParameterType;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public final class EmptyProtocolContext implements ProtocolContext {

  public static final EmptyProtocolContext INSTANCE = new EmptyProtocolContext();

  private EmptyProtocolContext() {
  }

  @Override
  public String protocolVersion() { return "2021"; }

  @Override
  public AddressMode addressMode() { return null; }

  @Override
  public List<? extends ProtocolPoint> points(ParameterType type) { return Collections.emptyList(); }

  @Override
  public Optional<String> property(String key) { return Optional.empty(); }
}
