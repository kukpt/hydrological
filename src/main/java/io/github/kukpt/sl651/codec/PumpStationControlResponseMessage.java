package io.github.kukpt.sl651.codec;

/**
 * @author shuo
 * 泵站控制响应消息
 */
public class PumpStationControlResponseMessage {

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
}
