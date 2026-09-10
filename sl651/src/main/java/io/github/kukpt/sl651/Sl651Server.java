package io.github.kukpt.sl651;

import io.github.kukpt.sl651.impl.Sl651ServerImpl;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;

public interface Sl651Server {

  static Sl651Server create(Vertx vertx, Sl651ServerOptions options) {
    return new Sl651ServerImpl(vertx, options);
  }

  Future<Sl651Server> listen(int port, String host);

  Future<Sl651Server> listen(int port);

  Future<Sl651Server> listen();

  Sl651Server endpointHandler(Handler<Sl651Endpoint> endpointHandler);

  Sl651Server exceptionHandler(Handler<Throwable> exceptionHandler);

  int actualPort();

  Future<Void> close();
}
