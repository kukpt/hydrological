package io.github.kukpt.sl651;

import org.junit.Test;

import io.vertx.core.json.JsonObject;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class Sl651ServerOptionsTest {

  @Test
  public void httpProxyIsDisabledByDefault() {
    assertFalse(new Sl651ServerOptions().isEnableHttpProxy());
  }

  @Test
  public void parsedTrafficDataIsEnabledByDefaultAndCanBeDisabled() {
    assertTrue(new Sl651ServerOptions().isIncludeParsedTrafficData());
    assertFalse(new Sl651ServerOptions().setIncludeParsedTrafficData(false)
      .isIncludeParsedTrafficData());
  }

  @Test
  public void parsedTrafficDataCanBeConfiguredFromJson() {
    Sl651ServerOptions options = new Sl651ServerOptions(
      new JsonObject().put("includeParsedTrafficData", false));

    assertFalse(options.isIncludeParsedTrafficData());
  }

  @Test
  public void httpProxyCanBeEnabledExplicitly() {
    assertTrue(new Sl651ServerOptions().enableHttpProxy(true).isEnableHttpProxy());
  }

  @Test
  public void httpProxyCanBeConfiguredFromJson() {
    Sl651ServerOptions options = new Sl651ServerOptions(
        new JsonObject().put("enableHttpProxy", true));

    assertTrue(options.isEnableHttpProxy());
  }

  @Test
  public void metricsWebCredentialsAreOptionalWhenMetricsWebIsDisabled() {
    new Sl651ServerOptions()
        .setMetricsWebUserName(null)
        .setMetricsWebPassword(null)
        .validateMetricsWebCredentials();
  }

  @Test(expected = IllegalArgumentException.class)
  public void metricsWebUsernameIsRequiredWhenMetricsWebIsEnabled() {
    new Sl651ServerOptions()
        .enableMetricsWeb(true)
        .setMetricsWebUserName(null)
        .setMetricsWebPassword("long-enough-password")
        .validateMetricsWebCredentials();
  }

  @Test(expected = IllegalArgumentException.class)
  public void metricsWebUsernameCannotBeBlankWhenMetricsWebIsEnabled() {
    new Sl651ServerOptions()
        .enableMetricsWeb(true)
        .setMetricsWebUserName("   ")
        .setMetricsWebPassword("long-enough-password")
        .validateMetricsWebCredentials();
  }

  @Test(expected = IllegalArgumentException.class)
  public void metricsWebPasswordIsRequiredWhenMetricsWebIsEnabled() {
    new Sl651ServerOptions()
        .enableMetricsWeb(true)
        .setMetricsWebUserName("admin")
        .setMetricsWebPassword(null)
        .validateMetricsWebCredentials();
  }

  @Test(expected = IllegalArgumentException.class)
  public void metricsWebPasswordMustBeLongEnough() {
    new Sl651ServerOptions()
        .enableMetricsWeb(true)
        .setMetricsWebUserName("admin")
        .setMetricsWebPassword("short")
        .validateMetricsWebCredentials();
  }

  @Test
  public void metricsWebCredentialsPassWhenConfigured() {
    new Sl651ServerOptions()
        .enableMetricsWeb(true)
        .setMetricsWebUserName("admin")
        .setMetricsWebPassword("long-enough-password")
        .validateMetricsWebCredentials();
  }
}
