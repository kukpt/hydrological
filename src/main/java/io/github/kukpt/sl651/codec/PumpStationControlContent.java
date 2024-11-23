package io.github.kukpt.sl651.codec;

import io.github.kukpt.sl651.utils.HydroLogicalUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * 泵站控制
 */
public class PumpStationControlContent implements DownstreamMessageContent{

  public PumpStationControlContent(int streamId, ReportTime reportTime, short length, short command){
    this.streamId = streamId;
    this.reportTime = reportTime;
    this.length = length;
    this.command = command;
  }

  private final int streamId;

  private final ReportTime reportTime;

  /**
   * 后续字节长度
   */
  private final short length;

  /**
   * 控制命令
   */
  private final short command;
  @Override
  public ByteBuf getByteBuf() {
    byte[] rtb = HydroLogicalUtils.strToBcd(reportTime.reportTimeStr());
    ByteBuf buf = Unpooled.buffer();
    buf.writeShort(streamId);
    buf.writeBytes(rtb);
    buf.writeByte(length);
    buf.writeByte(command);
    return buf;
  }
}
