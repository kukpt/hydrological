package io.github.kukpt.sl651.metrics;

import io.github.kukpt.hydrological.common.EventBusProtocolTrafficRecorder;
import io.github.kukpt.hydrological.monitoring.HttpProtocolDetector;
import io.github.kukpt.sl651.Sl651ServerOptions;
import io.github.kukpt.sl651.codec.Sl651Decoder;
import io.github.kukpt.sl651.codec.Sl651Encoder;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.timeout.IdleStateHandler;
import io.vertx.core.Vertx;
/** SL651 pipeline selector backed by the shared monitoring Web detector. */
public class ProtocolDetector extends HttpProtocolDetector {

  public ProtocolDetector(Vertx vertx, Sl651ServerOptions options) {
    super(vertx, pipeline -> installSl651Pipeline(vertx, pipeline, options));
  }

  private static void installSl651Pipeline(
      Vertx vertx, ChannelPipeline pipeline, Sl651ServerOptions options) {
    pipeline.addBefore("handler", "frame-decode",
      new LengthFieldBasedFrameDecoder(0xFFFF, 11, 2, 4, 0));
    pipeline.addBefore("handler", "idle",
      new IdleStateHandler(options.getTimeoutOnConnect(), 0, 0));
    pipeline.addBefore("handler", "hydrological-encode", new Sl651Encoder());
    pipeline.addBefore("handler", "hydrological-decode", new Sl651Decoder(
      new EventBusProtocolTrafficRecorder(vertx), options.isIncludeParsedTrafficData()));
  }
}
