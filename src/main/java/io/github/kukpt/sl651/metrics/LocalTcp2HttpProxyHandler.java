package io.github.kukpt.sl651.metrics;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetClientOptions;

import java.util.List;


public class LocalTcp2HttpProxyHandler extends ByteToMessageDecoder {

  private final static Logger log = LoggerFactory.getLogger(LocalTcp2HttpProxyHandler.class);

  private final Vertx vertx;

  private final int originPort;

  private final NetClientOptions options;

  private final NetClient netClient;

  public LocalTcp2HttpProxyHandler(Vertx vertx, int originPort) {
    this.vertx = vertx;
    this.originPort = originPort;
    options = new NetClientOptions();
    options.setConnectTimeout(30);
    this.netClient = vertx.createNetClient();
  }

  @Override
  protected void decode(ChannelHandlerContext ctx, ByteBuf buf, List<Object> list) throws Exception {
    byte[] bytes = new byte[buf.readableBytes()];
    buf.readBytes(bytes);
    Buffer buffer = Buffer.buffer(bytes);
    netClient.connect(originPort, "127.0.0.1")
             .onSuccess(originSocket -> {
               log.debug(String.format("Proxy Net Connected. LocalAddr %s", originSocket.localAddress()));
               originSocket.write(buffer);
               originSocket.handler(b -> {
                 ByteBuf byteBuf = Unpooled.buffer().writeBytes(b.getBytes());
                 ctx.writeAndFlush(byteBuf);
               });
               originSocket.closeHandler(unused -> {
                 log.debug("Proxy Net Client Closed.");
               });
             });
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    super.channelInactive(ctx);
    netClient.close();
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    super.exceptionCaught(ctx, cause);
    netClient.close();
    ctx.close();
  }
}
