package io.github.kukpt.sl651.codec;

import io.github.kukpt.sl651.message.IMessageBody;
import io.github.kukpt.sl651.utils.Sl651CodecUtils;
import io.netty.buffer.ByteBuf;

/**
 * @author shuo
 * 泵站控制响应消息
 */
public class PumpStationControlResponseMessage implements IMessageBody {

  private final int streamId;

  private final ReportTime reportTime;

  private final String telemetryStationAddress;

  private final short length;

  private final short command;

  public int streamId() {
    return streamId;
  }

  public ReportTime reportTime() {
    return reportTime;
  }

  public String telemetryStationAddress() {
    return telemetryStationAddress;
  }

  public short length() {
    return length;
  }

  public short command() {
    return command;
  }

  public PumpStationControlResponseMessage(
    int streamId, ReportTime reportTime, String telemetryStationAddress, short length, short command) {
    this.streamId = streamId;
    this.reportTime = reportTime;
    this.telemetryStationAddress = telemetryStationAddress;
    this.length = length;
    this.command = command;
  }

  public PumpStationControlResponseMessage(ByteBuf byteBuf) {
    this.streamId = byteBuf.readUnsignedShort();
    this.reportTime = Sl651CodecUtils.readReportTimeStr(byteBuf);
    this.telemetryStationAddress = Sl651CodecUtils.readTelemetryStationAddressSkipElementId(byteBuf);
    this.length = byteBuf.readUnsignedByte();
    this.command = byteBuf.readUnsignedByte();
  }
}
