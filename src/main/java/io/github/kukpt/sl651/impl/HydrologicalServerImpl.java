package io.github.kukpt.sl651.impl;

import io.github.kukpt.sl651.HydrologicalEndpoint;
import io.github.kukpt.sl651.HydrologicalServer;
import io.github.kukpt.sl651.HydrologicalServerOptions;
import io.github.kukpt.sl651.codec.HydrologicalDecode;
import io.github.kukpt.sl651.codec.HydrologicalDownstreamMessageEncode;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.ReferenceCountUtil;
import io.vertx.core.*;
import io.vertx.core.net.NetServer;
import io.vertx.core.net.impl.NetSocketInternal;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 水文协议服务器
 */
public class HydrologicalServerImpl implements HydrologicalServer {

  private final AtomicLong connectionCount = new AtomicLong(0L);

  private final HydrologicalServerOptions options;

  private final Vertx vertx;

  private final NetServer server;

  private Handler<HydrologicalEndpoint> endpointHandler;

  private Handler<Throwable> exceptionHandler;

  public HydrologicalServerImpl(Vertx vertx, HydrologicalServerOptions options) {
    this.vertx = vertx;
    this.options = options;
    this.server = vertx.createNetServer(options);
  }


  private void initChannel(ChannelPipeline pipeline) {
    // the SL651-2014 max frame length is 0xFFF
    pipeline.addBefore("handler", "frame-decode", new LengthFieldBasedFrameDecoder(0xfff, 11, 2, 4, 0));
    pipeline.addBefore("handler", "hydrological-encode", new HydrologicalDownstreamMessageEncode());
    pipeline.addBefore("handler", "hydrological-decode", new HydrologicalDecode());
    pipeline.addBefore("handler", "idle", new IdleStateHandler(options.getTimeoutOnConnect(), 0, 0));
  }

  @Override
  public Future<HydrologicalServer> listen(int port) {
    return listen(port, this.options.getHost());
  }

  @Override
  public Future<HydrologicalServer> listen() {
    return listen(this.options.getPort());
  }

  @Override
  public Future<HydrologicalServer> listen(int port, String host) {
    Handler<HydrologicalEndpoint> h1 = endpointHandler;
    Handler<Throwable> h2 = exceptionHandler;
    if (h1 == null) {
      return Future.failedFuture(new IllegalStateException("Please set handler before server is listening"));
    }
    server.connectHandler(so -> {
      NetSocketInternal soi = (NetSocketInternal) so;
      ChannelPipeline pipeline = soi.channelHandlerContext().pipeline();
      initChannel(pipeline);
      HydrologicalServerConnection conn = new HydrologicalServerConnection(soi, h1, h2, options);
      connectionCount.incrementAndGet();
      soi.eventHandler(ReferenceCountUtil::release);
      soi.messageHandler(msg -> {
        synchronized (conn) {
          conn.handleMsg(msg);
        }
      });
      soi.closeHandler(unused -> {
        connectionCount.decrementAndGet();
//        synchronized (conn) {
//          conn.handleClose();
//        }
      });
    });
    return server.listen(port, host).map(this);
  }

  @Override
  public synchronized HydrologicalServer endpointHandler(Handler<HydrologicalEndpoint> handler) {
    endpointHandler = handler;
    return this;
  }

  @Override
  public synchronized HydrologicalServer exceptionHandler(Handler<Throwable> handler) {
    exceptionHandler = handler;
    return this;
  }

  @Override
  public int actualPort() {
    return server.actualPort();
  }

  @Override
  public long connectionCount() {
    return this.connectionCount.get();
  }
  @Override
  public Future<Void> close() {
    return server.close();
  }


}
