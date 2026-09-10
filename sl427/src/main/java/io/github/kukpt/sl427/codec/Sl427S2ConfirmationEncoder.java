package io.github.kukpt.sl427.codec;

import io.github.kukpt.sl427.frame.ControlField;
import io.github.kukpt.sl427.frame.Direction;
import io.github.kukpt.sl427.frame.Sl427Frame;
import io.github.kukpt.sl427.frame.TimeTag;
import io.github.kukpt.sl427.model.TerminalWorkMode;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;

/** Builds center-station confirmation frames for SL427 S2 self-reports. */
public final class Sl427S2ConfirmationEncoder {
  private final Sl427FrameEncoder frameEncoder;
  private final Clock clock;
  private final int delayMinutes;

  public Sl427S2ConfirmationEncoder() {
    this(Clock.systemDefaultZone(), 0);
  }

  public Sl427S2ConfirmationEncoder(Clock clock, int delayMinutes) {
    if (delayMinutes < 0 || delayMinutes > 255) {
      throw new IllegalArgumentException("delayMinutes must be in 0..255");
    }
    this.clock = Objects.requireNonNull(clock, "clock");
    this.delayMinutes = delayMinutes;
    this.frameEncoder = new Sl427FrameEncoder();
  }

  public boolean supports(Sl427Frame report) {
    if (report == null || report.control().direction() != Direction.UPSTREAM
        || report.control().typeCode() == 0) {
      return false;
    }
    int afn = report.afn().value();
    return afn == 0x81 || afn == 0x82 || afn == 0x83 || afn == 0x84 || afn == 0xC0;
  }

  public byte[] encode(Sl427Frame report, TerminalWorkMode workMode) {
    return encode(report, workMode, LocalDateTime.now(clock));
  }

  public byte[] encode(Sl427Frame report, TerminalWorkMode workMode, LocalDateTime timestamp) {
    if (!supports(report)) {
      throw new IllegalArgumentException("frame is not an upstream S2 self-report");
    }
    Objects.requireNonNull(workMode, "workMode");

    // DIR=0, DIV=0, D3..D0=0000; return the corresponding two-bit FCB value.
    ControlField control = ControlField.from(report.control().frameCount() << 4);
    byte[] timeTag = TimeTag.of(timestamp, delayMinutes).encode();
    byte[] body = new byte[1 + timeTag.length];
    body[0] = (byte) workMode.code();
    System.arraycopy(timeTag, 0, body, 1, timeTag.length);
    return frameEncoder.encode(control, null, report.address(), report.afn(), body);
  }
}
