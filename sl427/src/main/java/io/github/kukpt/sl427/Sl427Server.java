package io.github.kukpt.sl427;

import io.github.kukpt.sl427.impl.Sl427ServerImpl;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;

import java.util.Collection;

/** SL/T 427-2021 TCP server. */
public interface Sl427Server {

  static Sl427Server create(Vertx vertx, Sl427ServerOptions options) {
    return new Sl427ServerImpl(vertx, options);
  }

  Future<Sl427Server> listen(int port, String host);

  Future<Sl427Server> listen(int port);

  Future<Sl427Server> listen();

  Sl427Server endpointHandler(Handler<Sl427Endpoint> endpointHandler);

  Sl427Server exceptionHandler(Handler<Throwable> exceptionHandler);

  /** Called once when a connection is identified as an online station. */
  Sl427Server onlineHandler(Handler<Sl427Endpoint> onlineHandler);

  /** Called when an identified station connection closes. */
  Sl427Server offlineHandler(Handler<Sl427Endpoint> offlineHandler);

  Sl427Endpoint getEndpoint(String endpointId);

  Collection<Sl427Endpoint> onlineEndpoints();

  int actualPort();

  Future<Void> close();
}
