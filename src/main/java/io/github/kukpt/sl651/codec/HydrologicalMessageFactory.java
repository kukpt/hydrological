package io.github.kukpt.sl651.codec;

import io.github.kukpt.sl651.utils.HydroLogicalUtils;
import io.netty.handler.codec.DecoderResult;

public class HydrologicalMessageFactory {

  public static HydrologicalDownstreamMessage createM2Ack(MessageHeader header, int streamId) {
    return new HydrologicalDownstreamMessage(header, new M2LinkModeAckMessage(streamId), HydroLogicalUtils.EOT);
  }

  public static HydrologicalMessage newMessage(MessageHeader header, Object payload) {
    return new HydrologicalMessage(header, payload, DecoderResult.SUCCESS);
  }

  public static HydrologicalMessage newInvalidMessage(MessageHeader header, Throwable cause) {
    return new HydrologicalMessage(header, null, DecoderResult.failure(cause));
  }
}
