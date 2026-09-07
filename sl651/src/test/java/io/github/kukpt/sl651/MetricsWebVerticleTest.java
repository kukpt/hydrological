package io.github.kukpt.sl651;

import io.vertx.core.Vertx;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class MetricsWebVerticleTest {

  private Vertx vertx;
  private int port;

  @Before
  public void deploy() throws Exception {
    vertx = Vertx.vertx();
    vertx.deployVerticle(new MetricsWebVerticle(
        "metrics-user", "correct-password", new OnlineEndpointRegistry()))
      .toCompletionStage().toCompletableFuture().get(10, TimeUnit.SECONDS);
    port = (int) vertx.sharedData().getLocalMap("hy").get("metrics-port");
  }

  @After
  public void close() throws Exception {
    if (vertx != null) {
      vertx.close().toCompletionStage().toCompletableFuture().get(10, TimeUnit.SECONDS);
    }
  }

  @Test
  public void validFormCredentialsCreateAuthenticatedSession() throws Exception {
    String form = "username=" + encode("metrics-user")
      + "&password=" + encode("correct-password");
    HttpURLConnection login = connection("/do-login");
    login.setInstanceFollowRedirects(false);
    login.setRequestMethod("POST");
    login.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
    login.setDoOutput(true);
    login.getOutputStream().write(form.getBytes(StandardCharsets.UTF_8));

    assertEquals(302, login.getResponseCode());
    assertEquals("/private/dashboard.html", login.getHeaderField("Location"));
    String cookie = login.getHeaderField("Set-Cookie");
    assertNotNull(cookie);

    HttpURLConnection dashboard = connection("/private/dashboard.html");
    dashboard.setInstanceFollowRedirects(false);
    dashboard.setRequestProperty("Cookie", cookie.split(";", 2)[0]);
    assertEquals(200, dashboard.getResponseCode());
  }

  private HttpURLConnection connection(String path) throws IOException {
    return (HttpURLConnection) new URL("http://127.0.0.1:" + port + path).openConnection();
  }

  private static String encode(String value) throws Exception {
    return URLEncoder.encode(value, "UTF-8");
  }
}
