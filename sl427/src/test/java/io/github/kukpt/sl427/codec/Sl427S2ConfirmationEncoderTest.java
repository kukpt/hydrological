package io.github.kukpt.sl427.codec;

import io.github.kukpt.sl427.Sl427Parser;
import io.github.kukpt.sl427.buffer.Hex;
import io.github.kukpt.sl427.frame.Direction;
import io.github.kukpt.sl427.frame.Sl427Frame;
import io.github.kukpt.sl427.model.CommandMessage;
import io.github.kukpt.sl427.model.ProtocolMessage;
import io.github.kukpt.sl427.model.TerminalWorkMode;
import io.github.kukpt.sl427.result.ParseResult;
import io.github.kukpt.sl427.schema.MessageRole;
import org.junit.Test;

import java.time.LocalDateTime;
import static org.junit.Assert.*;

public class Sl427S2ConfirmationEncoderTest {
  private static final byte[] REPORT = Hex.decode(
    "681C68B33200100110C0AAAAAAAAAA050100000000002000000010300322001216");

  @Test
  public void encodesC0ConfirmationWithMatchingFcbAddressAndAfn() {
    Sl427Frame report = new Sl427FrameParser().parse(REPORT).value();
    byte[] encoded = new Sl427S2ConfirmationEncoder().encode(report, TerminalWorkMode.COMPATIBLE,
      LocalDateTime.of(2026, 5, 4, 1, 2, 3));

    ParseResult<Sl427Frame> parsed = new Sl427FrameParser().parse(encoded);
    assertTrue(parsed.isSuccess());
    Sl427Frame confirmation = parsed.value();
    assertEquals(Direction.DOWNSTREAM, confirmation.control().direction());
    assertFalse(confirmation.control().split());
    assertEquals(report.control().frameCount(), confirmation.control().frameCount());
    assertEquals(0, confirmation.control().typeCode());
    assertArrayEquals(report.address().raw(), confirmation.address().raw());
    assertEquals(report.afn(), confirmation.afn());
    assertArrayEquals(Hex.decode("0003020104052600"), confirmation.bodyRaw());

    ParseResult<ProtocolMessage> message = new Sl427Parser().parse(encoded);
    assertTrue(message.isSuccess());
    assertEquals(MessageRole.CONFIRM, ((CommandMessage) message.value()).role());
    assertArrayEquals(new byte[] {0}, ((CommandMessage) message.value()).data());
  }

  @Test
  public void recognizesOnlyUpstreamSelfReports() {
    Sl427Frame report = new Sl427FrameParser().parse(REPORT).value();
    Sl427S2ConfirmationEncoder encoder = new Sl427S2ConfirmationEncoder();
    assertTrue(encoder.supports(report));

    byte[] query = new Sl427FrameEncoder().encode(
      io.github.kukpt.sl427.frame.ControlField.from(0x33), null, report.address(),
      io.github.kukpt.sl427.frame.Afn.standard(0xB0), new byte[0]);
    assertFalse(encoder.supports(new Sl427FrameParser().parse(query).value()));
  }
}
