package io.github.kukpt.sl427.assembly;

import io.github.kukpt.sl427.buffer.Hex;
import io.github.kukpt.sl427.codec.Sl427FrameEncoder;
import io.github.kukpt.sl427.codec.Sl427FrameParser;
import io.github.kukpt.sl427.frame.Afn;
import io.github.kukpt.sl427.frame.ControlField;
import io.github.kukpt.sl427.frame.Sl427Frame;
import io.github.kukpt.sl427.frame.StationAddress;
import io.github.kukpt.sl427.result.ParseResult;
import io.github.kukpt.sl427.result.ParseStatus;
import org.junit.Test;

import static org.junit.Assert.*;

public class Sl427FrameAssemblerTest {
  @Test
  public void assemblesCountdownFragments() {
    Sl427FrameEncoder encoder = new Sl427FrameEncoder();
    Sl427FrameParser parser = new Sl427FrameParser();
    StationAddress address = StationAddress.parse(Hex.decode("3200100110"));
    Sl427Frame first = parser.parse(encoder.encode(ControlField.from(0xF2), 2, address,
      Afn.standard(0xB0), new byte[] {1, 2})).value();
    Sl427Frame last = parser.parse(encoder.encode(ControlField.from(0xF2), 1, address,
      Afn.standard(0xB0), new byte[] {3, 4})).value();

    Sl427FrameAssembler assembler = new Sl427FrameAssembler();
    assertEquals(ParseStatus.PARTIAL, assembler.accept(first).status());
    ParseResult<Sl427Frame> result = assembler.accept(last);
    assertEquals(ParseStatus.SUCCESS, result.status());
    assertArrayEquals(new byte[] {1, 2, 3, 4}, result.value().bodyRaw());
    assertFalse(result.value().control().split());
  }

  @Test
  public void rejectsSkippedCountdown() {
    Sl427FrameEncoder encoder = new Sl427FrameEncoder();
    Sl427FrameParser parser = new Sl427FrameParser();
    StationAddress address = StationAddress.parse(Hex.decode("3200100110"));
    Sl427Frame first = parser.parse(encoder.encode(ControlField.from(0xF2), 3, address,
      Afn.standard(0xB0), new byte[] {1})).value();
    Sl427Frame last = parser.parse(encoder.encode(ControlField.from(0xF2), 1, address,
      Afn.standard(0xB0), new byte[] {2})).value();

    Sl427FrameAssembler assembler = new Sl427FrameAssembler();
    assembler.accept(first);
    ParseResult<Sl427Frame> result = assembler.accept(last);
    assertEquals(ParseStatus.INVALID_DATA, result.status());
    assertEquals("DIVS_SEQUENCE", result.issues().get(0).code());
  }
}
