package io.github.kukpt.sl651;

import io.github.kukpt.sl651.codec.ElementResult;
import io.github.kukpt.sl651.message.FixedBodyMessage;
import io.github.kukpt.sl651.codec.PayloadDecode;
import io.github.kukpt.sl651.message.TimingMessage;
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
import java.util.Collection;

@RunWith(VertxUnitRunner.class)
public class ServerDecodeTest extends ServerDecodeBase {

  @Before
  public void before(TestContext context) {
    super.setUp(endpoint -> {
      endpoint.messageHandler(m -> {
        m.payload();

        if (m.functionType() == 0x32) {
          // 定时报文
//          FixedBodyMessage fm = PayloadDecode.decodeFixedBodyMessage(m.payload());
//          Collection<ElementResult> elementResults = PayloadDecode.decodeDefaultElementResults(m.payload());
          TimingMessage t = m.timingPayload();
          System.out.println(t);
          t.elementResults().forEach(System.out::println);
        }
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
      String msg = "7e7e0137160200041234320031020016241101164502f1f1371602000448f0f02411011645ff012a0000000000ff022a000000000038121222452000ffc000039f9a";
      byte[] bytes = ByteBufUtil.decodeHexDump(msg, 0, msg.length());
      Buffer buffer = Buffer.buffer(bytes);
      so.write(buffer);
    });
  }

}
