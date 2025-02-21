package io.github.kukpt.sl651.codec;

import io.github.kukpt.sl651.utils.HydroLogicalUtils;
import io.netty.handler.codec.DecoderResult;

public class HydrologicalMessageFactory {

  public static DownstreamMessage createM2Ack(MessageHeader header, int streamId) {
    return new DownstreamMessage(header, new M2LinkModeAckMessage(streamId), HydroLogicalUtils.EOT);
  }

  public static UpstreamMessage newMessage(MessageHeader header, HydrologicalPayload payload, short frameEnd, int crcCode) {
    return new UpstreamMessage(header, payload, DecoderResult.SUCCESS, frameEnd);
  }

  public static UpstreamMessage newInvalidMessage(MessageHeader header, Throwable cause) {
    return new UpstreamMessage(header, null, DecoderResult.failure(cause), (short) 0);
  }
}
