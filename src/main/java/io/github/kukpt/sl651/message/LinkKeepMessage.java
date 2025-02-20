package io.github.kukpt.sl651.message;

import io.github.kukpt.sl651.codec.ReportTime;

public class LinkKeepMessage {

  private final int streamId;

  private final ReportTime reportTime;

  public LinkKeepMessage(int streamId, ReportTime reportTime) {
    this.streamId = streamId;
    this.reportTime = reportTime;
  }
}
