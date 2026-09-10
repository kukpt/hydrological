package io.github.kukpt.sl427.schema;

import io.github.kukpt.sl427.frame.TimeTag;

public final class BodyParts {

  private final byte[] data;
  private final byte[] password;
  private final TimeTag timeTag;

  public BodyParts(byte[] data, byte[] password, TimeTag timeTag) {
    this.data = data.clone();
    this.password = password == null ? null : password.clone();
    this.timeTag = timeTag;
  }

  public byte[] data() { return data.clone(); }
  public byte[] password() { return password == null ? null : password.clone(); }
  public TimeTag timeTag() { return timeTag; }
}
