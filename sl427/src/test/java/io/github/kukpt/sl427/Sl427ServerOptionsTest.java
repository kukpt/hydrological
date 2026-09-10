package io.github.kukpt.sl427;

import io.github.kukpt.sl427.buffer.Hex;
import io.github.kukpt.sl427.context.EmptyProtocolContext;
import io.github.kukpt.sl427.context.SimpleProtocolContext;
import io.github.kukpt.sl427.frame.StationAddress;
import io.github.kukpt.sl427.frame.AddressMode;
import io.github.kukpt.sl427.model.TerminalWorkMode;
import io.vertx.core.json.JsonObject;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class Sl427ServerOptionsTest {

  @Test
  public void usesServerDefaults() {
    Sl427ServerOptions options = new Sl427ServerOptions();

    assertEquals(11824, options.getPort());
    assertEquals("0.0.0.0", options.getHost());
    assertEquals(TerminalWorkMode.COMPATIBLE, options.getConfirmationWorkMode());
    assertEquals(AddressMode.ADMINISTRATIVE, options.getAddressMode());
    assertTrue(options.isAutoConfirmation());
    assertEquals(180, options.getTimeoutOnConnect());
    assertTrue(options.isEnableTrafficStore());
    assertFalse(options.isEnableMetricsWeb());
    assertFalse(options.isEnableHttpProxy());
    assertTrue(options.isIncludeParsedTrafficData());
    assertSame(EmptyProtocolContext.INSTANCE, options.getProtocolContext());
  }

  @Test
  public void parsedTrafficDataCanBeDisabledFromJson() {
    Sl427ServerOptions options = new Sl427ServerOptions(
      new JsonObject().put("includeParsedTrafficData", false));

    assertFalse(options.isIncludeParsedTrafficData());
  }

  @Test
  public void monitoringWebAndHttpProxyCanBeEnabledIndependently() {
    Sl427ServerOptions options = new Sl427ServerOptions()
      .enableMetricsWeb(true)
      .enableHttpProxy(true)
      .setMetricsWebUserName("admin")
      .setMetricsWebPassword("long-enough-password");

    assertTrue(options.isEnableMetricsWeb());
    assertTrue(options.isEnableHttpProxy());
    options.validateMetricsWebCredentials();
  }

  @Test
  public void readsAddressModeFromJson() {
    Sl427ServerOptions options = new Sl427ServerOptions(
      new JsonObject().put("addressMode", "STATION_CODE"));

    assertEquals(AddressMode.STATION_CODE, options.getAddressMode());
  }

  @Test
  public void resolverTakesPriorityOverFixedContext() {
    SimpleProtocolContext fixed = SimpleProtocolContext.builder().property("source", "fixed").build();
    SimpleProtocolContext resolved = SimpleProtocolContext.builder().property("source", "resolver").build();
    StationAddress address = StationAddress.parse(Hex.decode("3200100110"));

    Sl427ServerOptions options = new Sl427ServerOptions()
      .setProtocolContext(fixed)
      .setProtocolContextResolver(ignored -> resolved);

    assertSame(resolved, options.resolveProtocolContext(address));
  }
}
