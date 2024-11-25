package io.github.kukpt.sl651.codec;


import io.netty.buffer.ByteBuf;

public class TimeStep {

  static TimeStep createTimeStep(ByteBuf byteBuf) {
    byte[] timeStepBytes = new byte[3];
    byteBuf.readBytes(timeStepBytes);
    return new TimeStep(timeStepBytes);
  }

  private final Dhm dhm;

  private TimeStep(byte[] bytes) {
    this.dhm = new Dhm(bytes);
  }

  private int getDays() {
    return dhm.days;
  }

  private int getHours() {
    return dhm.hours;
  }

  private int getMinutes() {
    return dhm.minutes;
  }

  private static class Dhm {

    private final int days;

    private final int hours;

    private final int minutes;

    public Dhm(byte[] bytes) {
      this.days = bytes[0];
      this.hours = bytes[1];
      this.minutes = bytes[2];
    }
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("TimeStep{");
    sb.append("dhm=").append(dhm);
    sb.append('}');
    return sb.toString();
  }
}
