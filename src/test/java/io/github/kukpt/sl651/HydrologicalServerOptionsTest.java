package io.github.kukpt.sl651;

import org.junit.Test;

public class HydrologicalServerOptionsTest {

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
