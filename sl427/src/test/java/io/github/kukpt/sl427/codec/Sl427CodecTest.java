package io.github.kukpt.sl427.codec;

import io.github.kukpt.sl427.buffer.Hex;
import io.github.kukpt.sl427.checksum.Sl427Crc8;
import io.github.kukpt.sl427.frame.Sl427Frame;
import io.github.kukpt.sl427.frame.AddressMode;
import io.github.kukpt.sl427.frame.StationAddress;
import io.github.kukpt.sl427.result.ParseResult;
import io.github.kukpt.sl427.result.ParseStatus;
import io.vertx.core.buffer.Buffer;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class Sl427CodecTest {
  private static final byte[] GOLDEN = Hex.decode(
    "681C68B33200100110C0AAAAAAAAAA050100000000002000000010300322001216");

  @Test
  public void verifiesGoldenCrcAndHeader() {
    assertEquals(0x12, Sl427Crc8.calculate(GOLDEN, 3, 28));
    ParseResult<Sl427Frame> result = new Sl427FrameParser().parse(GOLDEN);
    assertEquals(ParseStatus.SUCCESS, result.status());
    assertEquals(28, result.value().declaredLength());
    assertEquals(0xC0, result.value().afn().value());
    assertEquals(3, result.value().control().typeCode());
    assertEquals(AddressMode.ADMINISTRATIVE, result.value().address().mode());
    assertEquals("3200100110", result.value().address().canonicalKey());
  }

  @Test
  public void rejectsCrcMismatch() {
    byte[] damaged = GOLDEN.clone();
    damaged[10] ^= 1;
    ParseResult<Sl427Frame> result = new Sl427FrameParser().parse(damaged);
    assertEquals(ParseStatus.INVALID_FRAME, result.status());
    assertEquals("CRC_MISMATCH", result.issues().get(0).code());
  }

  @Test
  public void parsesStationCodeOnlyWhenConfigured() {
    StationAddress address = StationAddress.parse(
      Hex.decode("0001020304"), AddressMode.STATION_CODE);

    assertEquals("01020304", address.stationCode());
    assertEquals("0001020304", address.canonicalKey());
  }

  @Test
  public void handlesNoiseHalfPacketAndStickyPackets() {
    Sl427StreamDecoder decoder = new Sl427StreamDecoder();
    byte[] firstHalf = new byte[12];
    System.arraycopy(GOLDEN, 0, firstHalf, 0, firstHalf.length);
    assertTrue(decoder.decode(Buffer.buffer(new byte[] {1, 2, 3}).appendBytes(firstHalf)).isEmpty());

    Buffer restAndAnother = Buffer.buffer();
    restAndAnother.appendBytes(GOLDEN, firstHalf.length, GOLDEN.length - firstHalf.length);
    restAndAnother.appendBytes(GOLDEN);
    List<byte[]> frames = decoder.decode(restAndAnother);
    assertEquals(2, frames.size());
    assertArrayEquals(GOLDEN, frames.get(0));
    assertArrayEquals(GOLDEN, frames.get(1));
  }
}
