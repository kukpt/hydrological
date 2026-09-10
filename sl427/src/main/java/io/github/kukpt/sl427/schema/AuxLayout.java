package io.github.kukpt.sl427.schema;

public enum AuxLayout {
  NONE(0, false, false),
  PASSWORD_2(2, true, false),
  TIME_TAG_7(7, false, true),
  PASSWORD_2_AND_TIME_TAG_7(9, true, true);

  private final int length;
  private final boolean password;
  private final boolean timeTag;

  AuxLayout(int length, boolean password, boolean timeTag) {
    this.length = length;
    this.password = password;
    this.timeTag = timeTag;
  }

  public int length() { return length; }
  public boolean hasPassword() { return password; }
  public boolean hasTimeTag() { return timeTag; }
}
