package io.github.kukpt.sl651;

import org.junit.Test;

import io.vertx.core.json.JsonObject;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class HydrologicalServerOptionsTest {

  @Test
  public void httpProxyIsDisabledByDefault() {
    assertFalse(new HydrologicalServerOptions().isEnableHttpProxy());
  }

  @Test
  public void httpProxyCanBeEnabledExplicitly() {
    assertTrue(new HydrologicalServerOptions().enableHttpProxy(true).isEnableHttpProxy());
  }

  @Test
  public void httpProxyCanBeConfiguredFromJson() {
    HydrologicalServerOptions options = new HydrologicalServerOptions(
        new JsonObject().put("enableHttpProxy", true));

    assertTrue(options.isEnableHttpProxy());
  }

  @Test
  public void metricsWebCredentialsAreOptionalWhenMetricsWebIsDisabled() {
    new HydrologicalServerOptions()
        .setMetricsWebUserName(null)
        .setMetricsWebPassword(null)
        .validateMetricsWebCredentials();
  }

  @Test(expected = IllegalArgumentException.class)
  public void metricsWebUsernameIsRequiredWhenMetricsWebIsEnabled() {
    new HydrologicalServerOptions()
        .enableMetricsWeb(true)
        .setMetricsWebUserName(null)
        .setMetricsWebPassword("long-enough-password")
        .validateMetricsWebCredentials();
  }

  @Test(expected = IllegalArgumentException.class)
  public void metricsWebUsernameCannotBeBlankWhenMetricsWebIsEnabled() {
    new HydrologicalServerOptions()
        .enableMetricsWeb(true)
        .setMetricsWebUserName("   ")
        .setMetricsWebPassword("long-enough-password")
        .validateMetricsWebCredentials();
  }

  @Test(expected = IllegalArgumentException.class)
  public void metricsWebPasswordIsRequiredWhenMetricsWebIsEnabled() {
    new HydrologicalServerOptions()
        .enableMetricsWeb(true)
        .setMetricsWebUserName("admin")
        .setMetricsWebPassword(null)
        .validateMetricsWebCredentials();
  }

  @Test(expected = IllegalArgumentException.class)
  public void metricsWebPasswordMustBeLongEnough() {
    new HydrologicalServerOptions()
        .enableMetricsWeb(true)
        .setMetricsWebUserName("admin")
        .setMetricsWebPassword("short")
        .validateMetricsWebCredentials();
  }

  @Test
  public void metricsWebCredentialsPassWhenConfigured() {
    new HydrologicalServerOptions()
        .enableMetricsWeb(true)
        .setMetricsWebUserName("admin")
        .setMetricsWebPassword("long-enough-password")
        .validateMetricsWebCredentials();
  }
}
