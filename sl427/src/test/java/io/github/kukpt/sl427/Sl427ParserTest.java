package io.github.kukpt.sl427;

import io.github.kukpt.hydrological.common.ProtocolTrafficRecord;
import io.github.kukpt.hydrological.common.ProtocolType;
import io.github.kukpt.sl427.buffer.Hex;
import io.github.kukpt.sl427.codec.Sl427FrameEncoder;
import io.github.kukpt.sl427.context.SimpleProtocolContext;
import io.github.kukpt.sl427.context.SimpleProtocolPoint;
import io.github.kukpt.sl427.frame.Afn;
import io.github.kukpt.sl427.frame.ControlField;
import io.github.kukpt.sl427.frame.StationAddress;
import io.github.kukpt.sl427.model.DataQuality;
import io.github.kukpt.sl427.model.CommandMessage;
import io.github.kukpt.sl427.model.LinkDetectionMessage;
import io.github.kukpt.sl427.model.LinkDetectionType;
import io.github.kukpt.sl427.model.ParameterType;
import io.github.kukpt.sl427.model.ProtocolMessage;
import io.github.kukpt.sl427.model.RawAfnMessage;
import io.github.kukpt.sl427.model.RealtimeDataMessage;
import io.github.kukpt.sl427.result.ParseResult;
import io.github.kukpt.sl427.result.ParseStatus;
import io.github.kukpt.sl427.schema.MessageRole;
import io.vertx.core.buffer.Buffer;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

public class Sl427ParserTest {
  private static final byte[] GOLDEN = Hex.decode(
    "681C68B33200100110C0AAAAAAAAAA050100000000002000000010300322001216");

  @Test
  public void parsesGoldenFlowReport() {
    ParseResult<ProtocolMessage> result = new Sl427Parser().parse(GOLDEN);

    assertEquals(ParseStatus.SUCCESS, result.status());
    RealtimeDataMessage message = (RealtimeDataMessage) result.value();
    assertEquals(ParameterType.FLOW_TOTAL, message.parameterType());
    assertEquals(2, message.measurements().size());
    assertEquals(ParameterType.FLOW, message.measurements().get(0).type());
    assertEquals(DataQuality.INVALID, message.measurements().get(0).quality());
    assertNull(message.measurements().get(0).value());
    assertEquals(ParameterType.TOTAL_WATER, message.measurements().get(1).type());
    assertEquals(new BigDecimal("105"), message.measurements().get(1).value());
    assertEquals(0x0000, message.status().alarmBits());
    assertEquals(0x0020, message.status().terminalBits());
    assertEquals(LocalDateTime.of(2022, 3, 30, 10, 0, 0), message.timeTag().timestamp());
    assertEquals(0, message.timeTag().delayMinutes());
  }

  @Test
  public void detectsConfiguredPointCountMismatch() {
    SimpleProtocolContext context = SimpleProtocolContext.builder()
      .addPoint(new SimpleProtocolPoint("flow-1", ParameterType.FLOW_TOTAL, 1, "m3/s"))
      .addPoint(new SimpleProtocolPoint("flow-2", ParameterType.FLOW_TOTAL, 2, "m3/s"))
      .build();

    ParseResult<ProtocolMessage> result = new Sl427Parser().parse(GOLDEN, context);
    assertEquals(ParseStatus.CONFIG_MISMATCH, result.status());
    assertEquals("POINT_COUNT_MISMATCH", result.issues().get(0).code());
  }

  @Test
  public void resolvesContextByDecodedStationAddress() {
    SimpleProtocolContext context = SimpleProtocolContext.builder()
      .addPoint(new SimpleProtocolPoint("flow-1", ParameterType.FLOW_TOTAL, 1, "m3/s"))
      .addPoint(new SimpleProtocolPoint("flow-2", ParameterType.FLOW_TOTAL, 2, "m3/s"))
      .build();
    AtomicReference<String> resolvedAddress = new AtomicReference<>();

    ParseResult<ProtocolMessage> result = new Sl427Parser().parse(GOLDEN, address -> {
      resolvedAddress.set(address.canonicalKey());
      return context;
    }, null);

    assertEquals("3200100110", resolvedAddress.get());
    assertEquals(ParseStatus.CONFIG_MISMATCH, result.status());
  }

  @Test
  public void parsesLinkDetection() {
    Sl427FrameEncoder encoder = new Sl427FrameEncoder();
    byte[] frame = encoder.encode(ControlField.from(0x80), null, address(), Afn.standard(0x02),
      new byte[] {(byte) 0xF2});

    ParseResult<ProtocolMessage> result = new Sl427Parser().parse(frame);
    assertEquals(ParseStatus.SUCCESS, result.status());
    assertEquals(LinkDetectionType.KEEP_ALIVE, ((LinkDetectionMessage) result.value()).type());
  }

  @Test
  public void leavesImageReportUnsupported() {
    byte[] frame = new Sl427FrameEncoder().encode(ControlField.from(0x8D), null, address(),
      Afn.standard(0x83), new byte[] {1, 2, 3});
    ParseResult<ProtocolMessage> result = new Sl427Parser().parse(frame);
    assertEquals(ParseStatus.UNSUPPORTED, result.status());
    assertTrue(result.value() instanceof RawAfnMessage);
    assertEquals("UNSUPPORTED_IMAGE_FRAME", result.issues().get(0).code());
  }

  @Test
  public void recordsEachCompleteProtocolFrameOnce() {
    List<ProtocolTrafficRecord> records = new ArrayList<>();
    Sl427Parser parser = new Sl427Parser("station-1", "127.0.0.1:10000", records::add);

    int split = GOLDEN.length / 2;
    assertTrue(parser.accept(Buffer.buffer(Arrays.copyOfRange(GOLDEN, 0, split))).isEmpty());
    assertEquals(1, parser.accept(Buffer.buffer(Arrays.copyOfRange(GOLDEN, split, GOLDEN.length))).size());

    assertEquals(1, records.size());
    assertEquals(ProtocolType.SL427, records.get(0).getProtocol());
    assertEquals("SL427", records.get(0).toJson().getString("protocol"));
    assertEquals("station-1", records.get(0).getEndpointId());
    assertEquals(Hex.encode(GOLDEN).toLowerCase(), records.get(0).getPayload());
    assertEquals("SUCCESS", records.get(0).getParseStatus());
    assertEquals("RealtimeDataMessage", records.get(0).getParsedData()
      .getString("messageType"));
    assertEquals("FLOW_TOTAL", records.get(0).getParsedData().getJsonObject("body")
      .getString("parameterType"));
  }

  @Test
  public void recordsInvalidFrameStatusWithoutParsedData() {
    byte[] damaged = Arrays.copyOf(GOLDEN, GOLDEN.length);
    damaged[damaged.length - 2] ^= 0x01;
    List<ProtocolTrafficRecord> records = new ArrayList<>();

    ParseResult<ProtocolMessage> result = new Sl427Parser(
      "station-1", "127.0.0.1:10000", records::add).parse(damaged);

    assertEquals(ParseStatus.INVALID_FRAME, result.status());
    assertEquals(1, records.size());
    assertEquals("INVALID_FRAME", records.get(0).getParseStatus());
    assertNull(records.get(0).getParsedData());
    assertNotNull(records.get(0).getParseError());
  }

  @Test
  public void recordsFirstFrameWithStableStationEndpointId() {
    List<ProtocolTrafficRecord> records = new ArrayList<>();
    Sl427Parser parser = new Sl427Parser(
      Sl427Endpoint::endpointId, "connection-1", "127.0.0.1:10000", records::add);

    parser.parse(GOLDEN);

    assertEquals(1, records.size());
    assertEquals("SL427:3200100110", records.get(0).getEndpointId());
  }

  @Test
  public void parsesManualReportForS2Confirmation() {
    byte[] body = Hex.decode("00000103020104052600");
    byte[] frame = new Sl427FrameEncoder().encode(ControlField.from(0x91), null, address(),
      Afn.standard(0x82), body);

    ParseResult<ProtocolMessage> result = new Sl427Parser().parse(frame);
    assertTrue(result.isSuccess());
    CommandMessage message = (CommandMessage) result.value();
    assertEquals(MessageRole.REPORT, message.role());
    assertArrayEquals(Hex.decode("000001"), message.data());
    assertEquals(LocalDateTime.of(2026, 5, 4, 1, 2, 3), message.timeTag().timestamp());
  }

  private static StationAddress address() {
    return StationAddress.parse(Hex.decode("3200100110"));
  }
}
