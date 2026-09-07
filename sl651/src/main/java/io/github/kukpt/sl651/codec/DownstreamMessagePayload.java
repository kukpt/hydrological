package io.github.kukpt.sl651.codec;

import io.netty.buffer.ByteBuf;

/**
 * @author shuo
 * 下行数据正文
 */
public interface DownstreamMessagePayload {

  ByteBuf getByteBuf();

}
