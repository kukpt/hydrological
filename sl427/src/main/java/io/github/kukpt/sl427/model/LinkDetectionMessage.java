package io.github.kukpt.sl427.model;

import io.github.kukpt.sl427.frame.Sl427Frame;

public final class LinkDetectionMessage implements ProtocolMessage {

  private final Sl427Frame frame;
  private final LinkDetectionType type;

  public LinkDetectionMessage(Sl427Frame frame, LinkDetectionType type) {
    this.frame = frame;
    this.type = type;
  }

  @Override
  public Sl427Frame frame() { return frame; }
  public LinkDetectionType type() { return type; }
}
