package io.github.kukpt.sl651.codec;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class ObservationTime {
  private final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyMMddHHmm");

  private final String observationTimeStr;

  public static ObservationTime now() {
    return new ObservationTime(LocalDateTime.now());
  }

  public LocalDateTime time() {
    return LocalDateTime.parse(observationTimeStr, FORMATTER);
  }

  public Long toEpochMilli() {
    return this.time().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
  }
  public ObservationTime(LocalDateTime time) {
    this.observationTimeStr = time.format(FORMATTER);
  }

  public ObservationTime(String observationTimeStr) {
    this.observationTimeStr =  observationTimeStr;
  }

  @Override
  public String toString() {
    return observationTimeStr;
  }
}
