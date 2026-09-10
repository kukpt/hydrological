package io.github.kukpt.sl651.impl;

import io.github.kukpt.sl651.*;
import io.github.kukpt.sl651.codec.Sl651Decoder;
import io.github.kukpt.sl651.codec.Sl651Encoder;
import io.github.kukpt.hydrological.common.ProtocolTrafficStoreVerticle;
import io.github.kukpt.hydrological.common.EventBusProtocolTrafficRecorder;
import io.github.kukpt.hydrological.monitoring.MonitoringWebOptions;
import io.github.kukpt.hydrological.monitoring.MonitoringWebVerticle;
import io.github.kukpt.sl651.metrics.ProtocolDetector;
import io.github.kukpt.sl651.metrics.TrafficMonitorHandler;
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
public class Sl651ServerImpl implements Sl651Server {

  private final static Logger log = LoggerFactory.getLogger(Sl651ServerImpl.class);

  private final Sl651ServerOptions options;

  private final Vertx vertx;

  private final NetServer server;

  private final Future<Void> initialization;

  private final OnlineEndpointRegistry onlineEndpoints = new OnlineEndpointRegistry();

  private final Sl651MonitoringProvider monitoringProvider;

  private Handler<Sl651Endpoint> endpointHandler;

  private Handler<Throwable> exceptionHandler;

  public Sl651ServerImpl(Vertx vertx, Sl651ServerOptions options) {
    this.vertx = vertx;
    this.options = options;
    this.server = vertx.createNetServer(options);
    this.monitoringProvider = new Sl651MonitoringProvider(vertx, onlineEndpoints);
    MonitoringWebVerticle.registerProvider(vertx, monitoringProvider);
    this.initialization = this.createSupportingServices(options);
  }

  private Future<Void> createSupportingServices(Sl651ServerOptions options) {
    return ProtocolTrafficStoreVerticle.deployShared(vertx, options.getMetricsLogBaseDir())
        .onSuccess(id -> log.info(String.format("deployed Message Store Server! id=%s", id)))
        .onFailure(err -> log.error(String.format(
            "deploy Message Store Server Verticle failed! %s", err.getMessage()), err))
        .compose(id -> {
          if (!options.isEnableMetricsWeb()) {
            return Future.succeededFuture();
          }
          options.validateMetricsWebCredentials();
          return MonitoringWebVerticle.deployShared(vertx,
              new MonitoringWebOptions()
                .setUserName(options.getMetricsWebUserName())
                .setPassword(options.getMetricsWebPassword()))
              .onSuccess(webId -> log.info(String.format(
                  "deployed Metrics Web Server! id=%s", webId)))
              .onFailure(err -> log.error(String.format(
                  "deploy Metrics Web Server Verticle failed! %s", err.getMessage()), err))
              .mapEmpty();
        });
  }

  private void initChannel(ChannelPipeline pipeline) {
    if (options.isEnableHttpProxy()) {
      pipeline.addBefore("handler", "protocol-detector", new ProtocolDetector(vertx, options));
    } else {
      // the SL651-2014 M2 max frame length is 0xFFF
      // M3 max frame length is 0xFFFF
      pipeline.addBefore("handler", "frame-decode", new LengthFieldBasedFrameDecoder(0xFFFF, 11, 2, 4, 0));
      pipeline.addBefore("handler", "idle", new IdleStateHandler(options.getTimeoutOnConnect(), 0, 0));
      pipeline.addBefore("handler", "hydrological-encode", new Sl651Encoder());
      pipeline.addBefore("handler", "hydrological-decode", new Sl651Decoder(
        new EventBusProtocolTrafficRecorder(vertx), options.isIncludeParsedTrafficData()));
    }
  }

  @Override
  public Future<Sl651Server> listen(int port) {
    return listen(port, this.options.getHost());
  }

  @Override
  public Future<Sl651Server> listen() {
    return listen(this.options.getPort());
  }

  @Override
  public Future<Sl651Server> listen(int port, String host) {
    Handler<Sl651Endpoint> h1 = endpointHandler;
    Handler<Throwable> h2 = exceptionHandler;
    if (h1 == null) {
      return Future.failedFuture(new IllegalStateException("Please set handler before server is listening"));
    }
    server.connectHandler(so -> {
      NetSocketInternal soi = (NetSocketInternal) so;
      ChannelPipeline pipeline = soi.channelHandlerContext().pipeline();
      initChannel(pipeline);
      Sl651ServerConnection conn = new Sl651ServerConnection(
        vertx, soi, h1, h2, options, onlineEndpoints);
      soi.eventHandler(ReferenceCountUtil::release);
      soi.messageHandler(msg -> {
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
    return initialization.compose(unused -> server.listen(port, host)).map(this);
  }

  @Override
  public synchronized Sl651Server endpointHandler(Handler<Sl651Endpoint> handler) {
    endpointHandler = handler;
    return this;
  }

  @Override
  public synchronized Sl651Server exceptionHandler(Handler<Throwable> handler) {
    exceptionHandler = handler;
    return this;
  }

  @Override
  public int actualPort() {
    return server.actualPort();
  }

  @Override
  public Future<Void> close() {
    return server.close().onComplete(unused ->
      MonitoringWebVerticle.unregisterProvider(vertx, monitoringProvider));
  }


}
