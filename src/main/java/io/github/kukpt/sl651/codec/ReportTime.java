package io.github.kukpt.sl651.codec;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class ReportTime {
  private final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyMMddHHmmss");

  private final String reportTimeStr;

  public static ReportTime now() {
    return new ReportTime(LocalDateTime.now());
  }

  public String reportTimeStr() {
    return this.reportTimeStr;
  }

  public LocalDateTime time() {
    return LocalDateTime.parse(reportTimeStr, FORMATTER);
  }

  public Long toEpochMilli() {
    return this.time().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
  }

  public ReportTime(LocalDateTime time) {
    this.reportTimeStr = time.format(FORMATTER);
  }

  public ReportTime(String reportTimeStr) {
    this.reportTimeStr = reportTimeStr;
  }

  @Override
  public String toString() {
    return reportTimeStr;
  }
}
