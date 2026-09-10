package io.github.kukpt.sl651;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetSocket;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import org.junit.After;

import java.util.concurrent.TimeUnit;


public class ServerDecodeBase {

  protected final int port = 11883;

  private Handler<Sl651Endpoint> hydrologicalEndpointHandler;

  Vertx vertx = Vertx.vertx();

  public void setUp(Handler<Sl651Endpoint> handler) {
    Sl651ServerOptions option = new Sl651ServerOptions().setPort(port);
    option.setLogActivity(true);
    try {
      Sl651Server.create(vertx, option)
                        .endpointHandler(handler)
                        .listen()
                        .toCompletionStage()
                        .toCompletableFuture()
                        .get(3, TimeUnit.SECONDS);
    } catch (Exception e) {
      throw new IllegalStateException("SL651 server failed to start", e);
    }
  }

  protected Future<NetSocket> connect(Handler<Buffer> handler) {
    return vertx.createNetClient().connect(port, "127.0.0.1")
                .map(nt -> nt.handler(handler::handle));
  }

  @After
  public void tearDown(TestContext context) {
    Async async = context.async();
    vertx.close().onComplete(context.asyncAssertSuccess(v -> async.complete()));
  }

}
