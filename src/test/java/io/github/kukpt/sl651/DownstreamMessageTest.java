package io.github.kukpt.sl651;

import io.github.kukpt.sl651.codec.DownstreamMessage;
import io.github.kukpt.sl651.codec.FunctionType;
import io.github.kukpt.sl651.codec.MessageHeader;
import io.github.kukpt.sl651.codec.PumpStationControlContent;
import io.github.kukpt.sl651.codec.RawDownstreamPayload;
import io.github.kukpt.sl651.codec.ReportTime;
import io.github.kukpt.sl651.utils.CRC16;
import io.github.kukpt.sl651.utils.HydroLogicalUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.atomic.AtomicBoolean;

@RunWith(VertxUnitRunner.class)
public class DownstreamMessageTest extends ServerDecodeBase {

  private static final String STATION_ADDRESS = "2222622222";

  private Async roundTripAsync;

  private boolean useGenericDownstream;

  @Before
  public void before(TestContext context) {
    AtomicBoolean sent = new AtomicBoolean();
    super.setUp(endpoint -> {
      endpoint.messageHandler(message -> {
        if (sent.compareAndSet(false, true)) {
          if (useGenericDownstream) {
            endpoint.downstream(
              FunctionType.PUMP_CONTROL,
              RawDownstreamPayload.fromHex("00002608111200000101"),
              3)
              .onSuccess(response -> {
                response.pumpStationControlResponseHandler(msg -> {
                  context.assertEquals(1, (int) msg.command());
                  context.assertEquals(STATION_ADDRESS, msg.telemetryStationAddress());
                  if (roundTripAsync != null) {
                    roundTripAsync.complete();
                  }
                });
                response.handle();
              })
              .onFailure(context::fail);
          } else {
            endpoint.pumpStationControl((short) 0x01, 3)
                    .onSuccess(response -> {
                      context.assertEquals(1, (int) response.command());
                      context.assertEquals(STATION_ADDRESS, response.telemetryStationAddress());
                      if (roundTripAsync != null) {
                        roundTripAsync.complete();
                      }
                    })
                    .onFailure(context::fail);
          }
        }
      });
    });
  }

  @Test
  public void encodePumpStationControl() {
    MessageHeader header = new MessageHeader(
      (short) 0x01,
      STATION_ADDRESS,
      0,
      FunctionType.PUMP_CONTROL.value(),
      0,
      HydroLogicalUtils.STX,
      0,
      0);
    PumpStationControlContent content =
      new PumpStationControlContent(0, new ReportTime("260811120000"), (short) 1, (short) 1);
    DownstreamMessage message = new DownstreamMessage(header, content, HydroLogicalUtils.ENQ);

    ByteBuf buf = Unpooled.buffer();
    ByteBuf headerBuf = message.messageHeader().downstreamBuf(message.content().getByteBuf().readableBytes());
    ByteBuf bodyBuf = message.content().getByteBuf();
    buf.writeBytes(headerBuf);
    buf.writeBytes(bodyBuf);
    buf.writeByte(message.frameControlType());
    buf.writeShort(CRC16.crc16(buf, buf.readableBytes()));

    org.junit.Assert.assertEquals(
      "7e7e22226222220100004c800a0200002608111200000101",
      ByteBufUtil.hexDump(buf, 0, buf.readableBytes() - 3));
    org.junit.Assert.assertEquals(HydroLogicalUtils.ENQ, buf.getUnsignedByte(buf.readableBytes() - 3));
    org.junit.Assert.assertEquals(CRC16.crc16(buf, buf.readableBytes() - 2), buf.getUnsignedShort(buf.readableBytes() - 2));

    headerBuf.release();
    bodyBuf.release();
    buf.release();
  }

  @Test(timeout = 5_000L)
  public void pumpStationControlRoundTrip(TestContext context) {
    useGenericDownstream = false;
    runDownstreamRoundTrip(context);
  }

  @Test(timeout = 5_000L)
  public void genericDownstreamRoundTrip(TestContext context) {
    useGenericDownstream = true;
    runDownstreamRoundTrip(context);
  }

  private void runDownstreamRoundTrip(TestContext context) {
    roundTripAsync = context.async();
    AtomicBoolean responded = new AtomicBoolean();

    super.connect(buffer -> {})
         .onSuccess(socket -> {
           socket.handler(buffer -> {
             byte[] bytes = buffer.getBytes();
             context.assertEquals(0x4C, bytes[10] & 0xFF);
             int bodyLength = (((bytes[11] & 0xFF) << 8) | (bytes[12] & 0xFF)) & 0x7FFF;
             if (bodyLength != 10 || !responded.compareAndSet(false, true)) {
               return;
             }
             int streamId = ((bytes[14] & 0xFF) << 8) | (bytes[15] & 0xFF);
             context.assertEquals(0, streamId);
             context.assertEquals(0x01, bytes[23] & 0xFF);
             context.assertEquals(CRC16.crc16(Unpooled.wrappedBuffer(bytes), bytes.length - 2),
                                  ((bytes[bytes.length - 2] & 0xFF) << 8) | (bytes[bytes.length - 1] & 0xFF));
             socket.write(Buffer.buffer(createPumpStationControlResponse(streamId)));
           });
           socket.write(Buffer.buffer(ByteBufUtil.decodeHexDump(
             "7e7e01222262222200002f00080201aa250219110641031ce4")));
         })
         .onFailure(context::fail);
  }

  private static byte[] createPumpStationControlResponse(int streamId) {
    ByteBuf buf = Unpooled.buffer();
    buf.writeShort(HydroLogicalUtils.FRAME_START_CHARACTER);
    buf.writeByte(0x01);
    buf.writeBytes(HydroLogicalUtils.strToBcd(STATION_ADDRESS));
    buf.writeShort(0);
    buf.writeByte(FunctionType.PUMP_CONTROL.value());
    buf.writeShort(0x0011);
    buf.writeByte(HydroLogicalUtils.STX);
    buf.writeShort(streamId);
    buf.writeBytes(HydroLogicalUtils.strToBcd("260811120001"));
    buf.writeBytes(HydroLogicalUtils.strToBcd("f1f1"));
    buf.writeBytes(HydroLogicalUtils.strToBcd(STATION_ADDRESS));
    buf.writeByte(1);
    buf.writeByte(1);
    buf.writeByte(HydroLogicalUtils.ETX);
    buf.writeShort(CRC16.crc16(buf, buf.readableBytes()));
    byte[] bytes = ByteBufUtil.getBytes(buf);
    buf.release();
    return bytes;
  }
}
