package io.github.kukpt.sl427.impl;

import io.github.kukpt.hydrological.common.EventBusProtocolTrafficRecorder;
import io.github.kukpt.hydrological.common.ProtocolTrafficRecorder;
import io.github.kukpt.hydrological.common.ProtocolTrafficStoreVerticle;
import io.github.kukpt.hydrological.monitoring.HttpProtocolDetector;
import io.github.kukpt.hydrological.monitoring.MonitoringWebOptions;
import io.github.kukpt.hydrological.monitoring.MonitoringWebVerticle;
import io.github.kukpt.sl427.Sl427Endpoint;
import io.github.kukpt.sl427.Sl427MonitoringProvider;
import io.github.kukpt.sl427.Sl427OnlineEndpointRegistry;
import io.github.kukpt.sl427.Sl427Server;
import io.github.kukpt.sl427.Sl427ServerOptions;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.net.NetServer;
import io.vertx.core.net.impl.NetSocketInternal;

import java.util.Objects;
import java.util.Collection;

/** Default Vert.x implementation of {@link Sl427Server}. */
public final class Sl427ServerImpl implements Sl427Server {

  private static final Logger log = LoggerFactory.getLogger(Sl427ServerImpl.class);

  private final Sl427ServerOptions options;
  private final Vertx vertx;
  private final NetServer server;
  private final Future<Void> initialization;
  private final ProtocolTrafficRecorder trafficRecorder;
  private final Sl427OnlineEndpointRegistry onlineEndpoints = new Sl427OnlineEndpointRegistry();
  private final Sl427MonitoringProvider monitoringProvider;
  private Handler<Sl427Endpoint> endpointHandler;
  private Handler<Throwable> exceptionHandler;
  private Handler<Sl427Endpoint> onlineHandler;
  private Handler<Sl427Endpoint> offlineHandler;

  public Sl427ServerImpl(Vertx vertx, Sl427ServerOptions options) {
    Objects.requireNonNull(vertx, "vertx");
    this.vertx = vertx;
    this.options = Objects.requireNonNull(options, "options");
    this.server = vertx.createNetServer(options);
    ProtocolTrafficRecorder configuredRecorder = options.getTrafficRecorder();
    this.monitoringProvider = new Sl427MonitoringProvider(onlineEndpoints);
    MonitoringWebVerticle.registerProvider(vertx, monitoringProvider);
    Future<Void> trafficInitialization;
    if (options.isEnableTrafficStore()) {
      ProtocolTrafficRecorder storeRecorder = new EventBusProtocolTrafficRecorder(vertx);
      this.trafficRecorder = configuredRecorder == ProtocolTrafficRecorder.NOOP
        ? storeRecorder
        : record -> {
          storeRecorder.record(record);
          configuredRecorder.record(record);
        };
      trafficInitialization = ProtocolTrafficStoreVerticle
        .deployShared(vertx, options.getTrafficLogBaseDir()).mapEmpty();
    } else {
      this.trafficRecorder = configuredRecorder;
      trafficInitialization = Future.succeededFuture();
    }
    this.initialization = trafficInitialization.compose(unused -> startMonitoringWeb());
  }

  @Override
  public Future<Sl427Server> listen(int port, String host) {
    Handler<Sl427Endpoint> endpointCallback = endpointHandler;
    if (endpointCallback == null) {
      return Future.failedFuture(new IllegalStateException(
        "Please set endpointHandler before server is listening"));
    }

    server.connectHandler(socket -> {
      if (options.isEnableHttpProxy()) {
        NetSocketInternal internal = (NetSocketInternal) socket;
        internal.channelHandlerContext().pipeline().addBefore(
          "handler", "protocol-detector", new HttpProtocolDetector(vertx, pipeline -> { }));
      }
      Sl427EndpointImpl endpoint = new Sl427EndpointImpl(
        socket, options, trafficRecorder, this::handleOnline, this::handleOffline);
      try {
        endpointCallback.handle(endpoint);
        endpoint.start();
      } catch (RuntimeException error) {
        handleException(error);
        socket.close();
      }
    });
    server.exceptionHandler(this::handleException);
    return initialization.compose(unused -> server.listen(port, host)).map(this);
  }

  private Future<Void> startMonitoringWeb() {
    if (!options.isEnableMetricsWeb()) return Future.succeededFuture();
    options.validateMetricsWebCredentials();
    return MonitoringWebVerticle.deployShared(vertx,
      new MonitoringWebOptions()
        .setUserName(options.getMetricsWebUserName())
        .setPassword(options.getMetricsWebPassword())).mapEmpty();
  }

  @Override
  public Future<Sl427Server> listen(int port) {
    return listen(port, options.getHost());
  }

  @Override
  public Future<Sl427Server> listen() {
    return listen(options.getPort(), options.getHost());
  }

  @Override
  public synchronized Sl427Server endpointHandler(Handler<Sl427Endpoint> handler) {
    endpointHandler = handler;
    return this;
  }

  @Override
  public synchronized Sl427Server exceptionHandler(Handler<Throwable> handler) {
    exceptionHandler = handler;
    return this;
  }

  @Override
  public synchronized Sl427Server onlineHandler(Handler<Sl427Endpoint> handler) {
    onlineHandler = handler;
    return this;
  }

  @Override
  public synchronized Sl427Server offlineHandler(Handler<Sl427Endpoint> handler) {
    offlineHandler = handler;
    return this;
  }

  @Override
  public Sl427Endpoint getEndpoint(String endpointId) {
    return onlineEndpoints.get(endpointId);
  }

  @Override
  public Collection<Sl427Endpoint> onlineEndpoints() {
    return onlineEndpoints.endpoints();
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

  private void handleException(Throwable error) {
    Handler<Throwable> handler = exceptionHandler;
    if (handler != null) {
      try {
        handler.handle(error);
        return;
      } catch (RuntimeException callbackError) {
        log.error("SL427 server exception handler failed", callbackError);
      }
    }
    log.error("SL427 server error", error);
  }

  private void handleOnline(Sl427Endpoint endpoint) {
    onlineEndpoints.register(endpoint);
    Handler<Sl427Endpoint> handler = onlineHandler;
    if (handler != null) invokeEndpointHandler(handler, endpoint);
  }

  private void handleOffline(Sl427Endpoint endpoint) {
    if (!onlineEndpoints.unregister(endpoint)) return;
    Handler<Sl427Endpoint> handler = offlineHandler;
    if (handler != null) invokeEndpointHandler(handler, endpoint);
  }

  private void invokeEndpointHandler(Handler<Sl427Endpoint> handler, Sl427Endpoint endpoint) {
    try {
      handler.handle(endpoint);
    } catch (RuntimeException error) {
      handleException(error);
    }
  }
}
