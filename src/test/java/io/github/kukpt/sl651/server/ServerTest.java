package io.github.kukpt.sl651.server;

import io.github.kukpt.sl651.HydrologicalServer;
import io.github.kukpt.sl651.HydrologicalServerOptions;
import io.netty.buffer.ByteBufUtil;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import org.junit.Test;


public class ServerTest {

  @Test
  public void e() throws InterruptedException {
    Vertx vertx = Vertx.vertx();
    HydrologicalServerOptions options = new HydrologicalServerOptions();
    options.setLogActivity(true);
    HydrologicalServer server = HydrologicalServer.create(vertx, options);
    server.endpointHandler(endpoint -> {
      endpoint.timingMessageHandler(timingMessage -> {
        System.err.println("TimingMessage!");
        System.err.println(timingMessage);
      });
      endpoint.closeHandler(unused -> {
        System.err.println("closed!");
      });
      endpoint.exceptionHandler(err -> {
        err.printStackTrace();
      });
    });
    server.exceptionHandler(err -> {
      System.err.println(err.getMessage());
    });
    server.listen();
    vertx.createNetClient().connect(11883, "127.0.0.1")
      .onSuccess(so -> {
        String hex = "7e7e011898202401123432004302004b240806161049f1f1189820240148f0f02408061610282b0000000000ff012800000000004520000004e138121251371b000000392300000000ff020825ff190800035070";
        byte[] bytes = ByteBufUtil.decodeHexDump(hex, 0, hex.length());
        Buffer buffer = Buffer.buffer(bytes);
        so.write(buffer);
        so.handler(buf -> {
          System.err.println(ByteBufUtil.hexDump(buf.getBytes()));
        });
      });

    Thread.sleep(5000);
  }

}
