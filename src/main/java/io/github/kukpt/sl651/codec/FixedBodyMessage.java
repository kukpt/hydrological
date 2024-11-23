package io.github.kukpt.sl651.codec;

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

  FixedBodyMessage(
    int streamId, ReportTime reportTime, String telemetryStationAddress, int classificationCode,
    ObservationTime observationTime) {
    this.streamId = streamId;
    this.reportTime = reportTime;
    this.telemetryStationAddress = telemetryStationAddress;
    this.classificationCode = classificationCode;
    this.observationTime = observationTime;
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
