package io.github.kukpt.sl651.metrics;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class LogPathUtils {

  // user.dir/hy_data/{endpointId}/{yyyy_MM_dd}.bin

  private static final DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy_MM_dd");

  private static final String userDir = System.getProperty("user.dir");

  private static final String PREFIX = "/hy_data";


  private static String getDateFolderName() {
    LocalDate now = LocalDate.now();
    return now.format(df);
  }

  public static String getWriteFileName(String endpointId) {
    return userDir + PREFIX + "/" + endpointId + "/" + getDateFolderName() + ".bin";
  }
}
