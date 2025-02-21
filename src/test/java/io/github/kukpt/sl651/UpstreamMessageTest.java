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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


@RunWith(VertxUnitRunner.class)
public class UpstreamMessageTest extends ServerDecodeBase {

  @Before
  public void Add() throws Exception {
    super.setUp(endpoint -> {
      endpoint.messageHandler(m -> {
        m.linkKeepMessageHandler(k -> {
          System.out.println(k.toString());
        });
        m.timingMessageHandler(t -> {
          System.out.println(t.toString());
          t.elementResults().forEach(System.out::println);
        });
        m.hourlyMessageHandler(h -> {
          System.out.println(h.toString());
          h.elementResults().forEach(System.out::println);
        });
        m.additionalMessageHandler(a -> {
          System.out.println(a.toString());
          a.elementResults().forEach(System.out::println);
        });

        m.handle();
      });
    });
  }

  static List<String> msgs = new ArrayList<>();

  static {
    // 小时报
    msgs.add("7e7e011234567891123434005c026735241205110009f1f1123456789148f0f02412051005f5c0000000000000000000000000000000000000000000000000f0f02412051100282b0000000000ff01280000000000371b000000381212883923000000004520000004e103410d");
    // 链路维持
    msgs.add("7e7e01222262222200002f00080201aa250219110641031ce4");
    // 定时报
    msgs.add("7e7e01ccd7000085c05032003a02008c000101000015f1f1ccd700008548f0f00001010000272b000000000076280000000000361b0000003923000000004520000000033812122503b140");
    // 加报报
    msgs.add("7e7e01222262222200003300300201ab250219110649f1f1222262222248f0f0250219110622190000002019000000261900000039230000000038121160033971");
  }

  @Test
  public void test(TestContext ctx) {
    Async async = ctx.async();
    Handler<Buffer> h = b -> {
      System.out.print("接收到服务端响应：-> ");
      System.out.println(ByteBufUtil.hexDump(b.getBytes()));

    };
    super.connect(h)
         .onSuccess(so -> {

           for (String msg : msgs) {
             byte[] bytes = ByteBufUtil.decodeHexDump(msg, 0, msg.length());
             Buffer buffer = Buffer.buffer(bytes);
             so.write(buffer);
           }

         });
  }

}
