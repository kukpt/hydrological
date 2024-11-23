package io.github.kukpt.sl651;

import io.github.kukpt.sl651.impl.HydrologicalServerImpl;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;

public interface HydrologicalServer {

  static HydrologicalServer create(Vertx vertx, HydrologicalServerOptions options) {
    return new HydrologicalServerImpl(vertx, options);
  }

  Future<HydrologicalServer> listen(int port, String host);

  Future<HydrologicalServer> listen(int port);

  Future<HydrologicalServer> listen();

  HydrologicalServer endpointHandler(Handler<HydrologicalEndpoint> endpointHandler);

  HydrologicalServer exceptionHandler(Handler<Throwable> exceptionHandler);

  int actualPort();

  long connectionCount();

  Future<Void> close();
}
