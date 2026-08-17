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
import io.vertx.core.net.NetSocket;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;


public class LocalTcp2HttpProxyHandler extends ByteToMessageDecoder {

  private final static Logger log = LoggerFactory.getLogger(LocalTcp2HttpProxyHandler.class);

  private final Vertx vertx;

  private final int originPort;

  private final NetClientOptions options;

  private final NetClient netClient;

  private final Deque<Buffer> pendingWrites = new ArrayDeque<>();

  private NetSocket originSocket;

  private boolean connecting;

  public LocalTcp2HttpProxyHandler(Vertx vertx, int originPort) {
    this.vertx = vertx;
    this.originPort = originPort;
    options = new NetClientOptions();
    options.setConnectTimeout(30_000);
    this.netClient = vertx.createNetClient(options);
  }

  @Override
  protected void decode(ChannelHandlerContext ctx, ByteBuf buf, List<Object> list) throws Exception {
    byte[] bytes = new byte[buf.readableBytes()];
    buf.readBytes(bytes);
    Buffer buffer = Buffer.buffer(bytes);
    if (originSocket != null) {
      originSocket.write(buffer);
      return;
    }
    pendingWrites.add(buffer);
    if (connecting) {
      return;
    }
    connecting = true;
    netClient.connect(originPort, "127.0.0.1")
             .onSuccess(originSocket -> {
               connecting = false;
               this.originSocket = originSocket;
               log.debug(String.format("Proxy Net Connected. LocalAddr %s", originSocket.localAddress()));
               while (!pendingWrites.isEmpty()) {
                 originSocket.write(pendingWrites.removeFirst());
               }
               originSocket.handler(b -> {
                 ByteBuf byteBuf = Unpooled.buffer().writeBytes(b.getBytes());
                 ctx.writeAndFlush(byteBuf);
               });
               originSocket.closeHandler(unused -> {
                 log.debug("Proxy Net Client Closed.");
                 this.originSocket = null;
                 ctx.close();
               });
             })
             .onFailure(error -> {
               connecting = false;
               pendingWrites.clear();
               log.error("Proxy Net Connect Failed.", error);
               ctx.close();
             });
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    if (originSocket != null) {
      originSocket.close();
      originSocket = null;
    }
    pendingWrites.clear();
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
