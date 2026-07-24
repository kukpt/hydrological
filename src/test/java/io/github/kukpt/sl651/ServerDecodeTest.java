package io.github.kukpt.sl651;

import io.netty.buffer.ByteBufUtil;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;

@RunWith(VertxUnitRunner.class)
public class ServerDecodeTest extends ServerDecodeBase {

  @Before
  public void before(TestContext context) {
    super.setUp(endpoint -> {
      endpoint.messageHandler(m -> {
        m.timingMessageHandler(t -> {
          System.out.println(t);
          t.elementResults().forEach(System.out::println);
        });
        m.handle();
      });
    });

  }

  @Test(timeout = 3_000L)
  public void test(TestContext ctx) {
    Async async = ctx.async();
    Handler<Buffer> h = b -> {
      System.out.print("接收到服务端响应：-> ");
      System.out.println(ByteBufUtil.hexDump(b.getBytes()));
      byte[] addr = b.getBytes(2, 7);
      ctx.assertTrue(Arrays.equals(addr, new byte[]{0x21, 0x01, 0x11, 0x02, 0x12}));
      async.complete();
    };
    super.connect(h)
    .onSuccess(so -> {
      String msg = "7e7e017020468929a00032002f0201f0260723092000f1f1702046892948f0f02607230920371b000556272b000056590030330001177817733812138103f3a9";
      byte[] bytes = ByteBufUtil.decodeHexDump(msg, 0, msg.length());
      Buffer buffer = Buffer.buffer(bytes);
      so.write(buffer);
    });
  }

}
