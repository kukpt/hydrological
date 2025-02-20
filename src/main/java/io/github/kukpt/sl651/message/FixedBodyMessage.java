package io.github.kukpt.sl651.message;

import io.github.kukpt.sl651.codec.ObservationTime;
import io.github.kukpt.sl651.codec.ReportTime;
import io.github.kukpt.sl651.utils.HydroLogicalUtils;
import io.netty.buffer.ByteBuf;

public class FixedBodyMessage {
  public int streamId() {
    return streamId;
  }

  public ReportTime reportTime() {
    return reportTime;
  }

  public String telemetryStationAddress() {
    return telemetryStationAddress;
  }

  public int classificationCode() {
    return classificationCode;
  }

  public ObservationTime observationTime() {
    return observationTime;
  }

  private final int streamId;

  private final ReportTime reportTime;

  private final String telemetryStationAddress;

  /**
   * 遥测站分类码
   */
  private final int classificationCode;

  private final ObservationTime observationTime;

  public FixedBodyMessage(ByteBuf byteBuf) {
    this.streamId = byteBuf.readUnsignedShort();// stream id
    this.reportTime = HydroLogicalUtils.readReportTimeStr(byteBuf);
    this.telemetryStationAddress = HydroLogicalUtils.readTelemetryStationAddressSkipElementId(byteBuf);
    this.classificationCode = byteBuf.readUnsignedByte();// 分类码
    this.observationTime = HydroLogicalUtils.readObservationTimeSkipElementId(byteBuf);
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("FixedBodyMessage{");
    sb.append("streamId=").append(streamId);
    sb.append(", reportTime=").append(reportTime);
    sb.append(", telemetryStationAddress='").append(telemetryStationAddress).append('\'');
    sb.append(", classificationCode=").append(classificationCode);
    sb.append(", observationTime=").append(observationTime);
    sb.append('}');
    return sb.toString();
  }
}
