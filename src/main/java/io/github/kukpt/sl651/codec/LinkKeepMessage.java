package io.github.kukpt.sl651.codec;


public class LinkKeepMessage {

  private final int streamId;

  private final ReportTime reportTime;

  public LinkKeepMessage(int streamId, ReportTime reportTime) {
    this.streamId = streamId;
    this.reportTime = reportTime;
  }
}
