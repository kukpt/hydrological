package io.github.kukpt.sl427.frame;

public final class Sl427Frame {

  private final int declaredLength;
  private final ControlField control;
  private final Integer splitCountdown;
  private final StationAddress address;
  private final Afn afn;
  private final byte[] bodyRaw;
  private final int checksum;
  private final byte[] rawFrame;

  public Sl427Frame(int declaredLength, ControlField control, Integer splitCountdown,
                    StationAddress address, Afn afn, byte[] bodyRaw, int checksum, byte[] rawFrame) {
    this.declaredLength = declaredLength;
    this.control = control;
    this.splitCountdown = splitCountdown;
    this.address = address;
    this.afn = afn;
    this.bodyRaw = bodyRaw.clone();
    this.checksum = checksum;
    this.rawFrame = rawFrame.clone();
  }

  public int declaredLength() { return declaredLength; }
  public ControlField control() { return control; }
  public Integer splitCountdown() { return splitCountdown; }
  public StationAddress address() { return address; }
  public Afn afn() { return afn; }
  public byte[] bodyRaw() { return bodyRaw.clone(); }
  public int checksum() { return checksum; }
  public byte[] rawFrame() { return rawFrame.clone(); }

  public Sl427Frame assembled(byte[] body, byte[] sourceFrames) {
    return new Sl427Frame(
      1 + 5 + afn.byteLength() + body.length,
      control.withoutSplit(), null, address, afn, body, checksum, sourceFrames);
  }
}
