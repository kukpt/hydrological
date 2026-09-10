package io.github.kukpt.sl427.frame;

import io.github.kukpt.sl427.buffer.Hex;

import java.util.Arrays;

public final class StationAddress {

  private final byte[] raw;
  private final AddressMode mode;
  private final String administrativeCode;
  private final Integer stationNumber;
  private final String stationCode;

  private StationAddress(byte[] raw, AddressMode mode, String administrativeCode,
                         Integer stationNumber, String stationCode) {
    this.raw = raw.clone();
    this.mode = mode;
    this.administrativeCode = administrativeCode;
    this.stationNumber = stationNumber;
    this.stationCode = stationCode;
  }

  public static StationAddress parse(byte[] raw) {
    return parse(raw, AddressMode.ADMINISTRATIVE);
  }

  public static StationAddress parse(byte[] raw, AddressMode mode) {
    if (raw == null || raw.length != 5) {
      throw new IllegalArgumentException("SL427 address must contain 5 bytes");
    }
    if (mode == null) {
      throw new IllegalArgumentException("SL427 address mode must not be null");
    }
    if (mode == AddressMode.STATION_CODE) {
      if ((raw[0] & 0xFF) != 0) {
        throw new IllegalArgumentException("station code address must start with 00");
      }
      return new StationAddress(raw, AddressMode.STATION_CODE, null, null, Hex.encode(Arrays.copyOfRange(raw, 1, 5)));
    }
    StringBuilder administrativeCode = new StringBuilder(6);
    for (int i = 0; i < 3; i++) {
      int high = (raw[i] >>> 4) & 0x0F;
      int low = raw[i] & 0x0F;
      if (high > 9 || low > 9) {
        throw new IllegalArgumentException("invalid administrative address BCD");
      }
      administrativeCode.append(high).append(low);
    }
    int stationNumber = (raw[3] & 0xFF) | ((raw[4] & 0xFF) << 8);
    return new StationAddress(raw, AddressMode.ADMINISTRATIVE,
      administrativeCode.toString(), stationNumber, null);
  }

  public byte[] raw() { return raw.clone(); }
  public AddressMode mode() { return mode; }
  public String administrativeCode() { return administrativeCode; }
  public Integer stationNumber() { return stationNumber; }
  public String stationCode() { return stationCode; }

  /** Returns the complete five-byte address as it appeared on the wire. */
  public String canonicalKey() {
    return Hex.encode(raw);
  }

  @Override
  public String toString() {
    return canonicalKey();
  }
}
