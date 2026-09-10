package io.github.kukpt.sl651;

import io.github.kukpt.hydrological.monitoring.MonitoringWebOptions;
import io.github.kukpt.hydrological.monitoring.MonitoringWebProxyTarget;
import io.github.kukpt.hydrological.monitoring.MonitoringWebVerticle;
import io.github.kukpt.hydrological.monitoring.ProtocolMonitoringProvider;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class MonitoringWebVerticleTest {

  private Vertx vertx;
  private int port;

  @Before
  public void deploy() throws Exception {
    vertx = Vertx.vertx();
    MonitoringWebVerticle.registerProvider(
      vertx, new Sl651MonitoringProvider(vertx, new OnlineEndpointRegistry()));
    MonitoringWebVerticle.registerProvider(vertx, new ProtocolMonitoringProvider() {
      @Override public String protocol() { return "SL427"; }
      @Override public JsonArray onlineEndpoints() {
        return new JsonArray().add(new JsonObject().put("endpointId", "SL427:test"));
      }
    });
    MonitoringWebVerticle.deployShared(vertx, new MonitoringWebOptions()
        .setUserName("metrics-user").setPassword("correct-password"))
      .toCompletionStage().toCompletableFuture().get(10, TimeUnit.SECONDS);
    port = MonitoringWebProxyTarget.port(vertx);
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

    HttpURLConnection endpoints = connection("/private/online-endpoints");
    endpoints.setRequestProperty("Cookie", cookie.split(";", 2)[0]);
    assertEquals(200, endpoints.getResponseCode());
    try (BufferedReader reader = new BufferedReader(
        new InputStreamReader(endpoints.getInputStream(), StandardCharsets.UTF_8))) {
      String body = reader.lines().collect(java.util.stream.Collectors.joining());
      assertTrue(body.contains("\"protocol\":\"SL427\""));
      assertTrue(body.contains("\"endpointId\":\"SL427:test\""));
    }
  }

  @Test
  public void proxiesHttpOnSl651ProtocolPortToSharedMonitoringWeb() throws Exception {
    Sl651Server server = Sl651Server.create(vertx, new Sl651ServerOptions()
        .setPort(0)
        .enableHttpProxy(true)
        .setMetricsLogBaseDir(Files.createTempDirectory("sl651-proxy-test-").toString()))
      .endpointHandler(endpoint -> { });
    try {
      server.listen().toCompletionStage().toCompletableFuture().get(5, TimeUnit.SECONDS);
      HttpURLConnection connection = (HttpURLConnection) new URL(
        "http://127.0.0.1:" + server.actualPort() + "/static/login.html").openConnection();
      connection.setConnectTimeout(5_000);
      connection.setReadTimeout(5_000);
      assertEquals(200, connection.getResponseCode());
    } finally {
      server.close().toCompletionStage().toCompletableFuture().get(5, TimeUnit.SECONDS);
    }
  }

  private HttpURLConnection connection(String path) throws IOException {
    return (HttpURLConnection) new URL("http://127.0.0.1:" + port + path).openConnection();
  }

  private static String encode(String value) throws Exception {
    return URLEncoder.encode(value, "UTF-8");
  }
}
