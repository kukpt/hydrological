package io.github.kukpt.sl651;

import io.netty.buffer.ByteBuf;
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
public class PictureMessageTest extends ServerDecodeBase{

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
      ctx.assertTrue(Arrays.equals(addr, new byte[]{0x37, 0x16, 0x02, 0x00, 0x04}));
      async.complete();
    };
    super.connect(h)
         .onSuccess(so -> {
           String msg = "7e7e01ccd7000085c05032003a02008c000101000015f1f1ccd700008548f0f00001010000272b000000000076280000000000361b0000003923000000004520000000033812122503b140";
           byte[] bytes = ByteBufUtil.decodeHexDump(msg, 0, msg.length());
           Buffer buffer = Buffer.buffer(bytes);
           so.write(buffer);
         });
  }

}
