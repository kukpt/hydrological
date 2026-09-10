package io.github.kukpt.sl427.frame;

public enum AddressMode {
  /** A1 (3-byte BCD administrative code) + A2 (2-byte binary station number). */
  ADMINISTRATIVE,
  /** 00 + 4-byte SL 502 station code. */
  STATION_CODE
}
