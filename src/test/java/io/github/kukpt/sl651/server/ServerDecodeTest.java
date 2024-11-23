package io.github.kukpt.sl651.server;

import io.github.kukpt.sl651.HydrologicalServer;
import io.github.kukpt.sl651.HydrologicalServerOptions;
import io.github.kukpt.sl651.codec.HydrologicalDecode;
import io.netty.buffer.ByteBufUtil;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

import io.vertx.ext.unit.junit.VertxUnitRunner;

@RunWith(VertxUnitRunner.class)
public class ServerDecodeTest {

  private Vertx vertx = Vertx.vertx();

  private HydrologicalDecode decode = new HydrologicalDecode();

  private final List<Object> out = new ArrayList<Object>();

  @Test
  public void testTimingMessage(TestContext context) {

    HydrologicalServer server = HydrologicalServer.create(vertx,
                                                                      new HydrologicalServerOptions().setPort(11883));
    server.exceptionHandler(t -> context.fail(t.toString()));
    server.endpointHandler(endpoint -> {
      endpoint.timingMessageHandler(tMsg -> {
        System.err.println(tMsg.toString());
        context.assertEquals("1898202401", tMsg.fixedBodyMessage().telemetryStationAddress());
      });
    });
    Async async = context.async();
    server.listen()
      .onComplete(context.asyncAssertSuccess(res -> {
        res.actualPort();

        vertx.createNetClient().connect(11883, "127.0.0.1")
             .onSuccess(so -> {
               String hex = "7e7e011898202401123432004302004b240806161049f1f1189820240148f0f02408061610282b0000000000ff012800000000004520000004e138121251371b000000392300000000ff020825ff190800035070";
               byte[] bytes = ByteBufUtil.decodeHexDump(hex, 0, hex.length());
               Buffer buffer = Buffer.buffer(bytes);
               so.write(buffer);
               so.handler(buf -> {
                 System.err.println(ByteBufUtil.hexDump(buf.getBytes()));
                 async.complete();
               });
             });

      }));
    async.awaitSuccess(15000);

  }
}
