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
      String msg = "7e7e022101110212a000320047020003250427193603f1f1210111021248f0f025042208453923aaaaaaaa361b000000272b0000000000302b0000000146ff042300000460ff052300061000ff1f11000038121366031369";
      byte[] bytes = ByteBufUtil.decodeHexDump(msg, 0, msg.length());
      Buffer buffer = Buffer.buffer(bytes);
      so.write(buffer);
    });
  }

}
