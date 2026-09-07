package io.github.kukpt.sl651;

import io.github.kukpt.sl651.codec.element.ElementDecodeUtils;
import io.github.kukpt.sl651.codec.element.ElementResult;
import io.github.kukpt.sl651.codec.element.ElementValueType;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collection;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

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

  @Test
  public void returnsRawValueWhenElementDecodingFails() {
    ByteBuf buf = Unpooled.wrappedBuffer(new byte[] {
      (byte) 0xF2, 0x18, 0x11, 0x22, 0x33
    });

    Collection<ElementResult> results = ElementDecodeUtils.decodeDefaultElementResults(buf);

    assertEquals(1, results.size());
    ElementResult result = results.iterator().next();
    assertEquals(0xF2, result.elementId().id());
    assertEquals(ElementValueType.BYTE_ARRAY, result.valueType());
    assertArrayEquals(new byte[] {0x11, 0x22, 0x33}, result.byteArrayValue());
    assertEquals(0, buf.readableBytes());
  }
}
