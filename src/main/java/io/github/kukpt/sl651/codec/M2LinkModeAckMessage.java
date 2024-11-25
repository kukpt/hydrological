package io.github.kukpt.sl651.codec;

import io.github.kukpt.sl651.utils.HydroLogicalUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class M2LinkModeAckMessage implements DownstreamMessageContent{

  private final int streamId;

  private final ReportTime reportTime;

  int streamId() {
    return this.streamId;
  }

  ReportTime reportTime() {
    return this.reportTime;
  }

  M2LinkModeAckMessage(int streamId) {
    this(streamId, ReportTime.now());
  }

  M2LinkModeAckMessage(int streamId, ReportTime reportTime) {
    this.streamId = streamId;
    this.reportTime = reportTime;
  }

  @Override
  public ByteBuf getByteBuf() {
    byte[] reportTime = HydroLogicalUtils.strToBcd(reportTime().reportTimeStr());
    ByteBuf buf = Unpooled.buffer();
    buf.writeShort(streamId);
    buf.writeBytes(reportTime);
    return buf;
  }
}
