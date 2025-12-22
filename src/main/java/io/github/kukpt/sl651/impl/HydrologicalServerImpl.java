package io.github.kukpt.sl651.impl;

import io.github.kukpt.sl651.*;
import io.github.kukpt.sl651.codec.HydrologicalDecode;
import io.github.kukpt.sl651.codec.HydrologicalEncode;
import io.github.kukpt.sl651.metrics.ProtocolDetector;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.ReferenceCountUtil;
import io.vertx.core.*;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.net.NetServer;
import io.vertx.core.net.impl.NetSocketInternal;


/**
 * 水文协议服务器
 */
public class HydrologicalServerImpl implements HydrologicalServer {

  private final static Logger log = LoggerFactory.getLogger(HydrologicalServerImpl.class);

  private final HydrologicalServerOptions options;

  private final Vertx vertx;

  private final NetServer server;

  private Handler<HydrologicalEndpoint> endpointHandler;

  private Handler<Throwable> exceptionHandler;

  public HydrologicalServerImpl(Vertx vertx, HydrologicalServerOptions options) {
    this.vertx = vertx;
    this.options = options;
    this.server = vertx.createNetServer(options);
    this.createMetricsWebServer(options);
  }
  private void createMetricsWebServer(HydrologicalServerOptions options) {
    if (options.isEnableMetricsWeb()) {
      vertx.deployVerticle(new MetricsWebVerticle(options.getMetricsWebUserName(),
                                                  options.getMetricsWebPassword()))
      .onSuccess(id -> {log.info(String.format("deployed Metrics Web Server! id=%s", id));})
      .onFailure(err -> {log.error(String.format("deploy Metrics Web Server Verticle failed! %s", err.getMessage()), err);});
    }
  }

  private void initChannel(ChannelPipeline pipeline) {
    if (options.isEnableMetricsWeb()) {
      pipeline.addBefore("handler", "protocol-detector", new ProtocolDetector(vertx, options));
    } else {
      // the SL651-2014 M2 max frame length is 0xFFF
      // M3 max frame length is 0xFFFF
      pipeline.addBefore("handler", "frame-decode", new LengthFieldBasedFrameDecoder(0xFFFF, 11, 2, 4, 0));
      pipeline.addBefore("handler", "idle", new IdleStateHandler(options.getTimeoutOnConnect(), 0, 0));
      pipeline.addBefore("handler", "hydrological-encode", new HydrologicalEncode());
      pipeline.addBefore("handler", "hydrological-decode", new HydrologicalDecode());
    }
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
      soi.eventHandler(ReferenceCountUtil::release);
      soi.messageHandler(msg -> {
        HydrologicalServerConnection conn = new HydrologicalServerConnection(vertx, soi, h1, h2, options);
        soi.closeHandler(unused -> {
          synchronized (conn) {
            conn.handleClose();
          }
        });
        synchronized (conn) {
          conn.handleMsg(msg);
        }
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
  public Future<Void> close() {
    return server.close();
  }


}
