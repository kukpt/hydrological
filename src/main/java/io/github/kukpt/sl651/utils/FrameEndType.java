package io.github.kukpt.sl651.utils;

public enum FrameEndType {

  EOT_MODE(HydroLogicalUtils.EOT),
  ACK_MODE(HydroLogicalUtils.ACK),
  ESC_MODE(HydroLogicalUtils.ESC);

  private final short value;

  public short value() {
    return value;
  }

  FrameEndType(short value) {
    this.value = value;
  }
}
