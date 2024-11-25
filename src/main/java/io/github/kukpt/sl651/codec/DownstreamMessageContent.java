package io.github.kukpt.sl651.codec;

import io.netty.buffer.ByteBuf;

public interface DownstreamMessageContent {

  ByteBuf getByteBuf();

}
