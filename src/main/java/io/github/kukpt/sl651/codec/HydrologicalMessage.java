package io.github.kukpt.sl651.codec;


import io.netty.handler.codec.DecoderResult;


public class HydrologicalMessage {

  private final MessageHeader header;

  private final Object payload;

  private final DecoderResult coderResult;

  public MessageHeader header() {
    return header;
  }

  public Object payload() {
    return payload;
  }

  public DecoderResult coderResult() {
    return coderResult;
  }

  HydrologicalMessage(MessageHeader header, Object payload, DecoderResult coderResult) {
    this.header = header;
    this.payload = payload;
    this.coderResult = coderResult;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("HydrologicalMessage{");
    sb.append("header=").append(header);
    sb.append(", payload=").append(payload);
    sb.append('}');
    return sb.toString();
  }
}
