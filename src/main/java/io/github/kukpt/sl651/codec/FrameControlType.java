package io.github.kukpt.sl651.codec;

public enum FrameControlType {

  STX(0x02);


  private final int value;

  FrameControlType(int value) {
    this.value = value;
  }
}
