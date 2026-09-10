package io.github.kukpt.sl427.model;

import io.github.kukpt.sl427.frame.Sl427Frame;

public final class RawAfnMessage implements ProtocolMessage {

  private final Sl427Frame frame;

  public RawAfnMessage(Sl427Frame frame) {
    this.frame = frame;
  }

  @Override
  public Sl427Frame frame() { return frame; }
  public byte[] body() { return frame.bodyRaw(); }
}
