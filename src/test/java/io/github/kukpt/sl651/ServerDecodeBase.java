package io.github.kukpt.sl651;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetServerOptions;
import io.vertx.core.net.NetSocket;


public class ServerDecodeBase {

  protected final int port = 11883;

  private Handler<HydrologicalEndpoint> hydrologicalEndpointHandler;

  Vertx vertx = Vertx.vertx();

  public void setUp(Handler<HydrologicalEndpoint> handler) {
    HydrologicalServerOptions option = new HydrologicalServerOptions().setPort(port);
    option.setLogActivity(true);
    HydrologicalServer.create(vertx, option)
                      .endpointHandler(handler).listen();
  }

  protected Future<NetSocket> connect(Handler<Buffer> handler) {
    return vertx.createNetClient().connect(port, "127.0.0.1")
                .map(nt -> nt.handler(handler::handle));
  }

}
