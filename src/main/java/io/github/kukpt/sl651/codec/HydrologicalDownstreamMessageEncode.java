package io.github.kukpt.sl651.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;

import java.util.List;

public class HydrologicalDownstreamMessageEncode extends MessageToMessageEncoder<HydrologicalDownstreamMessage> {

  @Override
  protected void encode(ChannelHandlerContext ctx, HydrologicalDownstreamMessage downstreamMessage, List<Object> out) throws Exception {
    ByteBuf buf = ctx.alloc().buffer();
    ByteBuf bodyBuf = downstreamMessage.content().getByteBuf();
    ByteBuf headerBuf = downstreamMessage.messageHeader().downstreamBuf(bodyBuf.readableBytes());
    buf.writeBytes(headerBuf);
    buf.writeBytes(bodyBuf);
    buf.writeByte(downstreamMessage.frameControlType());
    buf.writeShort(CRC16.crc16(buf, buf.readableBytes()));
    out.add(buf);
    headerBuf.release();
    bodyBuf.release();
  }
}
