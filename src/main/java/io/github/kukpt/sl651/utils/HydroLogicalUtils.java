package io.github.kukpt.sl651.utils;

import io.github.kukpt.sl651.codec.ObservationTime;
import io.github.kukpt.sl651.codec.ReportTime;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;

public class HydroLogicalUtils {


  public static final int FRAME_START_CHARACTER = 0x7E7E;

  public static final short ETX = 0x03;

  public static final short STX = 0x02;

  public static final short EOT = 0x04;

  public static final short ENQ = 0x05;


  private HydroLogicalUtils() {
  }

  public static ObservationTime readObservationTimeSkipElementId(ByteBuf buf) {
    buf.skipBytes(2);
    return readObservationTimeStr(buf);
  }

  public static String readTelemetryStationAddressSkipElementId(ByteBuf buf) {
    buf.skipBytes(2);
    return readTelemetryStationAddress(buf);
  }

  public static String readTelemetryStationAddress(ByteBuf buf) {
    return readString(buf, 5);
  }

  /**
   * @param buf
   * @return YYMMddHHmmSS
   */
  public static ReportTime readReportTimeStr(ByteBuf buf) {
    return new ReportTime(readString(buf, 6));
  }

  /**
   * @param buf
   * @return YYMMddHHmm
   */
  public static ObservationTime readObservationTimeStr(ByteBuf buf) {
    return new ObservationTime(readString(buf, 5));
  }

  public static String readString(ByteBuf buf, int length) {
    String s = ByteBufUtil.hexDump(buf, buf.readerIndex(), length);
    buf.skipBytes(length);
    return s;
  }

  public static double readBcdNumber(ByteBuf buf, int length, int numberPoint) {
    String bcd = readString(buf, length).toUpperCase();
    double dv;
    if (bcd.startsWith("FF")) {
      bcd = bcd.substring(2);
      dv = -Double.parseDouble(bcd);
    } else {
      dv = Double.parseDouble(bcd);
    }
    if (numberPoint > 0) {
      return dv / ptn(numberPoint);
    }
    return dv;
  }

  private static int ptn(int i) {
    i--;
    if (i < 0) {
      return 0;
    }
    if (i == 0) {
      return 10;
    }
    return 10 * ptn(i);
  }

  public static byte[] strToBcd(String asc) {
    int len = asc.length();
    int mod = len % 2;
    if (mod != 0) {
      asc = "0" + asc;
      len = asc.length();
    }

    if (len >= 2) {
      len >>= 1;
    }

    byte[] bbt = new byte[len];
    byte[] abt = asc.getBytes();

    for(int p = 0; p < asc.length() / 2; ++p) {
      int j;
      if (abt[2 * p] >= 48 && abt[2 * p] <= 57) {
        j = abt[2 * p] - 48;
      } else if (abt[2 * p] >= 97 && abt[2 * p] <= 122) {
        j = abt[2 * p] - 97 + 10;
      } else {
        j = abt[2 * p] - 65 + 10;
      }

      int k;
      if (abt[2 * p + 1] >= 48 && abt[2 * p + 1] <= 57) {
        k = abt[2 * p + 1] - 48;
      } else if (abt[2 * p + 1] >= 97 && abt[2 * p + 1] <= 122) {
        k = abt[2 * p + 1] - 97 + 10;
      } else {
        k = abt[2 * p + 1] - 65 + 10;
      }

      int a = (j << 4) + k;
      byte b = (byte)a;
      bbt[p] = b;
    }

    return bbt;
  }
}
