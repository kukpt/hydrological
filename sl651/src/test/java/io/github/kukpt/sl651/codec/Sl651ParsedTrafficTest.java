package io.github.kukpt.sl651.codec;

import io.github.kukpt.hydrological.common.ProtocolTrafficRecord;
import io.github.kukpt.sl651.message.IMessageBody;
import io.github.kukpt.sl651.message.LinkKeepMessage;
import io.github.kukpt.sl651.utils.CRC16;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.vertx.core.json.JsonObject;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertNotNull;

public class Sl651ParsedTrafficTest {

  private static final String LINK_KEEP =
    "7e7e01222262222200002f00080201aa250219110641031ce4";

  @Test
  public void recordsParsedDataAndReusesTheBodyForBusinessHandlers() {
    List<ProtocolTrafficRecord> records = new ArrayList<>();
    EmbeddedChannel channel = new EmbeddedChannel(new Sl651Decoder(records::add, true));

    channel.writeInbound(Unpooled.wrappedBuffer(ByteBufUtil.decodeHexDump(LINK_KEEP)));
    UpstreamMessage message = channel.readInbound();

    assertEquals(1, records.size());
    ProtocolTrafficRecord record = records.get(0);
    assertEquals("SUCCESS", record.getParseStatus());
    JsonObject parsed = record.getParsedData();
    assertEquals("LinkKeepMessage", parsed.getString("messageType"));
    assertEquals("2222622222", parsed.getJsonObject("header")
      .getString("telemetryStationAddress"));
    assertEquals(426, parsed.getJsonObject("body").getInteger("streamId").intValue());

    IMessageBody parsedBody = message.parseBody();
    AtomicReference<LinkKeepMessage> handled = new AtomicReference<>();
    message.linkKeepMessageHandler(handled::set).handle();
    assertSame(parsedBody, handled.get());

    channel.finishAndReleaseAll();
  }

  @Test
  public void bodyEnrichmentFailureDoesNotSuppressTheDecodedMessage() {
    byte[] source = ByteBufUtil.decodeHexDump(LINK_KEEP);
    byte[] shortBody = new byte[18];
    System.arraycopy(source, 0, shortBody, 0, 14);
    shortBody[11] = 0;
    shortBody[12] = 1;
    shortBody[14] = 1;
    shortBody[15] = 3;
    int crc = CRC16.crc16(Unpooled.wrappedBuffer(shortBody), shortBody.length - 2);
    shortBody[16] = (byte) (crc >>> 8);
    shortBody[17] = (byte) crc;

    List<ProtocolTrafficRecord> records = new ArrayList<>();
    EmbeddedChannel channel = new EmbeddedChannel(new Sl651Decoder(records::add, true));
    channel.writeInbound(Unpooled.wrappedBuffer(shortBody));

    assertNotNull(channel.readInbound());
    assertEquals("INVALID_DATA", records.get(0).getParseStatus());
    assertNotNull(records.get(0).getParseError());
    channel.finishAndReleaseAll();
  }
}
