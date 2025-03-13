package io.github.kukpt.sl651;

import io.github.kukpt.sl651.codec.element.ElementDecodeUtils;
import io.github.kukpt.sl651.codec.element.ElementResult;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collection;

@RunWith(VertxUnitRunner.class)
public class ElementDecodeTest {

  @Test
  public void test() {
    byte[] bytes = new byte[] {
    (byte) 0xFF,
    0x21,
    0x11,
    0x02,
    0x01
    };
    ByteBuf buf = Unpooled.buffer();
    buf.writeBytes(bytes);
    Collection<ElementResult> results = ElementDecodeUtils.decodeDefaultElementResults(buf);
    results.forEach(System.out::println);
  }
}
