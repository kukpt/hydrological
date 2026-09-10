package io.github.kukpt.sl427.codec;

import io.github.kukpt.sl427.checksum.Sl427Crc8;
import io.github.kukpt.sl427.frame.Afn;
import io.github.kukpt.sl427.frame.ControlField;
import io.github.kukpt.sl427.frame.StationAddress;

public final class Sl427FrameEncoder {

  public byte[] encode(ControlField control, Integer splitCountdown, StationAddress address,
                       Afn afn, byte[] body) {
    if (control.split() != (splitCountdown != null)) {
      throw new IllegalArgumentException("DIV and splitCountdown must agree");
    }
    if (splitCountdown != null && (splitCountdown < 1 || splitCountdown > 255)) {
      throw new IllegalArgumentException("splitCountdown must be in 1..255");
    }
    byte[] payload = body == null ? new byte[0] : body;
    int userLength = 1 + (control.split() ? 1 : 0) + 5 + afn.byteLength() + payload.length;
    if (userLength > 255) {
      throw new IllegalArgumentException("ordinary SL427 frame user data exceeds 255 bytes");
    }
    byte[] frame = new byte[userLength + 5];
    frame[0] = 0x68;
    frame[1] = (byte) userLength;
    frame[2] = 0x68;
    int position = 3;
    frame[position++] = (byte) control.raw();
    if (control.split()) {
      frame[position++] = (byte) (int) splitCountdown;
    }
    byte[] addressBytes = address.raw();
    System.arraycopy(addressBytes, 0, frame, position, addressBytes.length);
    position += addressBytes.length;
    if (afn.custom()) {
      frame[position++] = (byte) 0xFF;
      frame[position++] = (byte) (afn.value() & 0xFF);
    } else {
      frame[position++] = (byte) afn.value();
    }
    System.arraycopy(payload, 0, frame, position, payload.length);
    position += payload.length;
    frame[position++] = (byte) Sl427Crc8.calculate(frame, 3, userLength);
    frame[position] = 0x16;
    return frame;
  }
}
