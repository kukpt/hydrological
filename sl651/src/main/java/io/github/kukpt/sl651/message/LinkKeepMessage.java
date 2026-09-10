package io.github.kukpt.sl651.message;

import io.github.kukpt.sl651.codec.ReportTime;
import io.github.kukpt.sl651.utils.Sl651CodecUtils;
import io.netty.buffer.ByteBuf;

public class LinkKeepMessage implements IMessageBody {

  private final int streamId;

  private final ReportTime reportTime;

  public LinkKeepMessage(ByteBuf byteBuf) {
    this.streamId = byteBuf.readUnsignedShort();
    this.reportTime = Sl651CodecUtils.readReportTimeStr(byteBuf);
  }

  @Override
  public int streamId() {
    return this.streamId;
  }

  public ReportTime reportTime() {
    return reportTime;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("LinkKeepMessage{");
    sb.append("streamId=").append(streamId);
    sb.append(", reportTime=").append(reportTime);
    sb.append('}');
    return sb.toString();
  }
}
