package io.github.kukpt.sl651;

import io.vertx.core.Handler;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.nio.Buffer;

@RunWith(VertxUnitRunner.class)
public class FunctionTypeDecodeTest extends ServerDecodeBase {


  @Test
  public void testDecode(TestContext context) {

    Async async = context.async();
    Handler<Buffer> h = b -> {

    };
  }
}
