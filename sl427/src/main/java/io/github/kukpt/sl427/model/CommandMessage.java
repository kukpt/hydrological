package io.github.kukpt.sl427.model;

import io.github.kukpt.sl427.frame.Sl427Frame;
import io.github.kukpt.sl427.frame.TimeTag;
import io.github.kukpt.sl427.schema.MessageRole;

/** A decoded command/query/confirmation whose data has no dedicated business model yet. */
public final class CommandMessage implements ProtocolMessage {
  private final Sl427Frame frame;
  private final MessageRole role;
  private final byte[] data;
  private final TimeTag timeTag;

  public CommandMessage(Sl427Frame frame, MessageRole role, byte[] data, TimeTag timeTag) {
    this.frame = frame;
    this.role = role;
    this.data = data.clone();
    this.timeTag = timeTag;
  }

  @Override public Sl427Frame frame() { return frame; }
  public MessageRole role() { return role; }
  public byte[] data() { return data.clone(); }
  public TimeTag timeTag() { return timeTag; }
}
