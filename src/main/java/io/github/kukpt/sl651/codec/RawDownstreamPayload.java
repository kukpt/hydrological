package io.github.kukpt.sl651.codec;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;

/**
 * Raw downstream payload for commands that do not have a typed payload yet.
 */
public class RawDownstreamPayload implements DownstreamMessagePayload, StreamIdentifiedPayload {

  private final byte[] bytes;

  private RawDownstreamPayload(byte[] bytes) {
    this.bytes = bytes;
  }

  public static RawDownstreamPayload fromHex(String hex) {
    return new RawDownstreamPayload(ByteBufUtil.decodeHexDump(hex));
  }

  public static RawDownstreamPayload of(byte[] bytes) {
    byte[] copy = new byte[bytes.length];
    System.arraycopy(bytes, 0, copy, 0, bytes.length);
    return new RawDownstreamPayload(copy);
  }

  public static RawDownstreamPayload of(ByteBuf byteBuf) {
    byte[] bytes = ByteBufUtil.getBytes(byteBuf, byteBuf.readerIndex(), byteBuf.readableBytes(), false);
    return new RawDownstreamPayload(bytes);
  }

  @Override
  public ByteBuf getByteBuf() {
    return Unpooled.wrappedBuffer(bytes).copy();
  }

  @Override
  public int streamId() {
    if (bytes.length < 2) {
      return 0;
    }
    return ((bytes[0] & 0xFF) << 8) | (bytes[1] & 0xFF);
  }
}
