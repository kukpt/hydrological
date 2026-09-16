package io.github.kukpt.sl651;

import io.github.kukpt.sl651.codec.element.ElementDecodeUtils;
import io.github.kukpt.sl651.codec.element.ElementResult;
import io.github.kukpt.sl651.codec.element.ElementValueUtils;
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
    ElementResult result = results.iterator().next();
    assertEquals(ElementValueType.BYTE_ARRAY, result.valueType());
    assertArrayEquals(new byte[] {0x02, 0x01}, result.byteArrayValue());
    assertArrayEquals(new byte[] {0x02, 0x01}, result.rawValue());
    assertEquals(20.1, ElementValueUtils.doubleValue(
      result.rawValue(), result.elementId()), 0.000001);
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
    assertArrayEquals(new byte[] {0x11, 0x22, 0x33}, result.rawValue());
    assertEquals(0, buf.readableBytes());
  }

  @Test
  public void preservesRawValueForKnownDoubleElement() {
    ByteBuf buf = Unpooled.wrappedBuffer(new byte[] {
      0x20, 0x11, 0x02, 0x01
    });

    ElementResult result = ElementDecodeUtils.decodeDefaultElementResults(buf)
      .iterator().next();

    assertEquals(ElementValueType.DOUBLE, result.valueType());
    assertArrayEquals(new byte[] {0x02, 0x01}, result.rawValue());
    assertEquals(20.1, result.doubleValue(), 0.000001);
  }

  @Test
  public void fallsBackToRawValueWhenKnownDoubleCannotBeConverted() {
    ByteBuf buf = Unpooled.wrappedBuffer(new byte[] {
      0x20, 0x08, 0x1A
    });

    ElementResult result = ElementDecodeUtils.decodeDefaultElementResults(buf)
      .iterator().next();

    assertEquals(ElementValueType.BYTE_ARRAY, result.valueType());
    assertArrayEquals(new byte[] {0x1A}, result.byteArrayValue());
  }

  @Test
  public void convertsRawBytesToAsciiIntAndStatus32() {
    ByteBuf buf = Unpooled.wrappedBuffer(new byte[] {
      (byte) 0xFF, 0x21, 0x20, 0x31, 0x32, 0x33, 0x34
    });

    ElementResult result = ElementDecodeUtils.decodeDefaultElementResults(buf)
      .iterator().next();

    assertEquals("1234", result.asciiValue());
    assertEquals(0x31323334, result.intValue());
    assertEquals(0x31323334L, result.status32Value());
  }

  @Test
  public void convertsRawBytesToLongAndSmallStatusValues() {
    ByteBuf longBuf = Unpooled.wrappedBuffer(new byte[] {
      (byte) 0xFF, 0x21, 0x40,
      0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08
    });
    ElementResult longResult = ElementDecodeUtils
      .decodeDefaultElementResults(longBuf).iterator().next();
    assertEquals(0x0102030405060708L, longResult.longValue());

    ByteBuf status8Buf = Unpooled.wrappedBuffer(new byte[] {
      (byte) 0xFF, 0x21, 0x08, (byte) 0xFF
    });
    ElementResult status8Result = ElementDecodeUtils
      .decodeDefaultElementResults(status8Buf).iterator().next();
    assertEquals(255, status8Result.status8Value());

    ByteBuf status16Buf = Unpooled.wrappedBuffer(new byte[] {
      (byte) 0xFF, 0x21, 0x10, (byte) 0xFE, (byte) 0xDC
    });
    ElementResult status16Result = ElementDecodeUtils
      .decodeDefaultElementResults(status16Buf).iterator().next();
    assertEquals(0xFEDC, status16Result.status16Value());
  }
}
