package io.github.kukpt.sl651.utils;

public enum FrameEndType {

  EOT_MODE(Sl651CodecUtils.EOT),
  ACK_MODE(Sl651CodecUtils.ACK),
  ESC_MODE(Sl651CodecUtils.ESC);

  private final short value;

  public short value() {
    return value;
  }

  FrameEndType(short value) {
    this.value = value;
  }
}
