package io.github.kukpt.sl651.codec;


import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.handler.codec.DecoderResult;

import static io.github.kukpt.sl651.utils.HydroLogicalUtils.ETB;


public class HydrologicalMessage {

  private final MessageHeader header;

  private final HydrologicalPayload payload;

  private final DecoderResult coderResult;

  private final short frameEnd;

  public MessageHeader header() {
    return header;
  }

  public ByteBuf payload() {
    if (header().multiPack()) {
      return payload.mp().buffers();
    } else {
      return payload.sp();
    }
  }

  public DecoderResult coderResult() {
    return coderResult;
  }

  public boolean hasNextFrame() {
    return frameEnd == ETB;
  }


  HydrologicalMessage(MessageHeader header, HydrologicalPayload payload, DecoderResult coderResult, short frameEnd) {
    this.header = header;
    this.payload = payload;
    this.coderResult = coderResult;
    this.frameEnd = frameEnd;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("HydrologicalMessage{");
    sb.append("header=").append(header);
    byte[] bs = new byte[this.payload().readableBytes()];
    this.payload().getBytes(0, bs);
    sb.append(", payload=").append(ByteBufUtil.hexDump(bs));
    sb.append('}');
    return sb.toString();
  }
}
