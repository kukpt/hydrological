package io.github.kukpt.sl427;

import io.github.kukpt.sl427.buffer.Hex;
import io.github.kukpt.sl427.codec.Sl427FrameParser;
import io.github.kukpt.sl427.frame.Direction;
import io.github.kukpt.sl427.frame.Sl427Frame;
import io.github.kukpt.sl427.result.ParseResult;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetSocket;
import org.junit.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class Sl427MainVerticleTest {
  private static final byte[] GOLDEN = Hex.decode(
    "681C68B33200100110C0AAAAAAAAAA050100000000002000000010300322001216");

  @Test
  public void startsServerAndAcceptsSl427Frame() throws Exception {
    Vertx vertx = Vertx.vertx();
    Sl427MainVerticle verticle = new Sl427MainVerticle(
      new Sl427ServerOptions().setPort(0).setEnableTrafficStore(false));
    String deploymentId = null;
    try {
      deploymentId = vertx.deployVerticle(verticle).toCompletionStage().toCompletableFuture()
        .get(5, TimeUnit.SECONDS);
      assertTrue(verticle.actualPort() > 0);

      NetSocket socket = vertx.createNetClient()
        .connect(verticle.actualPort(), "127.0.0.1")
        .toCompletionStage().toCompletableFuture().get(5, TimeUnit.SECONDS);
      CompletableFuture<Buffer> confirmation = new CompletableFuture<>();
      socket.handler(confirmation::complete);
      socket.write(Buffer.buffer(GOLDEN)).toCompletionStage().toCompletableFuture()
        .get(5, TimeUnit.SECONDS);

      ParseResult<Sl427Frame> parsed = new Sl427FrameParser().parse(
        confirmation.get(5, TimeUnit.SECONDS).getBytes());
      assertTrue(parsed.isSuccess());
      assertEquals(Direction.DOWNSTREAM, parsed.value().control().direction());
      assertEquals(0, parsed.value().control().typeCode());
      assertEquals(0xC0, parsed.value().afn().value());
      socket.close().toCompletionStage().toCompletableFuture().get(5, TimeUnit.SECONDS);
    } finally {
      if (deploymentId != null) {
        vertx.undeploy(deploymentId).toCompletionStage().toCompletableFuture()
          .get(5, TimeUnit.SECONDS);
      }
      vertx.close().toCompletionStage().toCompletableFuture().get(5, TimeUnit.SECONDS);
    }
  }
}
