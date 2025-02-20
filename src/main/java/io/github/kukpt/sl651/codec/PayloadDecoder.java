package io.github.kukpt.sl651.codec;

import io.netty.buffer.ByteBuf;

import java.util.function.Function;

public abstract class PayloadDecoder<T> implements Function<ByteBuf, T> {


}
