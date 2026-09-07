package io.github.kukpt.sl651.metrics;

import io.github.kukpt.sl651.HydrologicalServerOptions;
import io.github.kukpt.sl651.codec.HydrologicalDecode;
import io.github.kukpt.sl651.codec.HydrologicalEncode;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.timeout.IdleStateHandler;
import io.vertx.core.Vertx;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;

import java.util.List;

public class ProtocolDetector extends ByteToMessageDecoder {

  private final static Logger log = LoggerFactory.getLogger(ProtocolDetector.class);


  private final Vertx vertx;

  private final HydrologicalServerOptions options;

  public ProtocolDetector(Vertx vertx, HydrologicalServerOptions options) {
    this.vertx = vertx;
    this.options = options;
  }
  @Override
  protected void decode(ChannelHandlerContext ctx, ByteBuf byteBuf, List<Object> list) throws Exception {
    if (byteBuf.readableBytes() < 5) {
      return;
    }
    if (isHttp(byteBuf)) {
      int port = (int) vertx.sharedData().getLocalMap("hy").get("metrics-port");
      ctx.pipeline().addBefore("handler", "http-proxy", new LocalTcp2HttpProxyHandler(vertx, port));
      ctx.pipeline().remove("handler");
    } else {
      // the SL651-2014 M2 max frame length is 0xFFF
      // M3 max frame length is 0xFFFF
      ctx.pipeline().addBefore("handler", "frame-decode", new LengthFieldBasedFrameDecoder(0xFFFF, 11, 2, 4, 0));
      ctx.pipeline().addBefore("handler", "idle", new IdleStateHandler(options.getTimeoutOnConnect(), 0, 0));
      ctx.pipeline().addBefore("handler", "hydrological-encode", new HydrologicalEncode());
      ctx.pipeline().addBefore("handler", "hydrological-decode", new HydrologicalDecode());

    }
    ctx.pipeline().remove(this);
    log.debug(String.format("after ProtocolDetector piplines %s", ctx.pipeline().toString()));
  }

  private boolean isHttp(ByteBuf in) {
    int magic1 = in.getUnsignedByte(in.readerIndex());
    int magic2 = in.getUnsignedByte(in.readerIndex() + 1);
    return (magic1 == 'G' && magic2 == 'E') || // GET
    (magic1 == 'P' && magic2 == 'O');   // POST
  }
}
