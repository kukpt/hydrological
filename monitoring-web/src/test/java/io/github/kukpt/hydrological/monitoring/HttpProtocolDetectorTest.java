package io.github.kukpt.hydrological.monitoring;

import io.netty.buffer.Unpooled;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class HttpProtocolDetectorTest {
  @Test
  public void recognizesCommonHttpMethods() {
    assertTrue(http("GET / HTTP/1.1\r\n"));
    assertTrue(http("POST /do-login HTTP/1.1\r\n"));
    assertTrue(http("HEAD / HTTP/1.1\r\n"));
    assertTrue(http("OPTIONS / HTTP/1.1\r\n"));
  }

  @Test
  public void doesNotTreatProtocolBytesAsHttp() {
    assertFalse(HttpProtocolDetector.isHttp(
      Unpooled.wrappedBuffer(new byte[] {0x68, 0x32, 0x00, 0x10, 0x01})));
  }

  private static boolean http(String value) {
    return HttpProtocolDetector.isHttp(Unpooled.wrappedBuffer(
      value.getBytes(StandardCharsets.US_ASCII)));
  }
}
