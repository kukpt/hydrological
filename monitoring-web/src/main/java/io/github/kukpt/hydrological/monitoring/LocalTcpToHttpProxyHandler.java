package io.github.kukpt.hydrological.monitoring;

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

/** Bridges an HTTP connection accepted on a protocol TCP port to the local monitoring server. */
public class LocalTcpToHttpProxyHandler extends ByteToMessageDecoder {
  private static final Logger log = LoggerFactory.getLogger(LocalTcpToHttpProxyHandler.class);

  private final NetClient netClient;
  private final String originHost;
  private final int originPort;
  private final Deque<Buffer> pendingWrites = new ArrayDeque<>();
  private NetSocket originSocket;
  private boolean connecting;

  public LocalTcpToHttpProxyHandler(Vertx vertx, int originPort) {
    this(vertx, "127.0.0.1", originPort);
  }

  public LocalTcpToHttpProxyHandler(Vertx vertx, String originHost, int originPort) {
    this.originHost = originHost;
    this.originPort = originPort;
    this.netClient = vertx.createNetClient(new NetClientOptions().setConnectTimeout(30_000));
  }

  @Override
  protected void decode(ChannelHandlerContext ctx, ByteBuf buf, List<Object> out) {
    byte[] bytes = new byte[buf.readableBytes()];
    buf.readBytes(bytes);
    Buffer buffer = Buffer.buffer(bytes);
    if (originSocket != null) {
      originSocket.write(buffer);
      return;
    }
    pendingWrites.add(buffer);
    if (connecting) return;
    connecting = true;
    netClient.connect(originPort, originHost)
      .onSuccess(socket -> {
        connecting = false;
        originSocket = socket;
        while (!pendingWrites.isEmpty()) socket.write(pendingWrites.removeFirst());
        socket.handler(data -> ctx.writeAndFlush(Unpooled.wrappedBuffer(data.getBytes())));
        socket.closeHandler(unused -> {
          originSocket = null;
          ctx.close();
        });
      })
      .onFailure(error -> {
        connecting = false;
        pendingWrites.clear();
        log.error("Cannot connect protocol-port HTTP proxy to monitoring Web", error);
        ctx.close();
      });
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    if (originSocket != null) originSocket.close();
    originSocket = null;
    pendingWrites.clear();
    netClient.close();
    super.channelInactive(ctx);
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    netClient.close();
    ctx.close();
  }
}
