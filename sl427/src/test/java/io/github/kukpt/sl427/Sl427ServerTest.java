package io.github.kukpt.sl427;

import io.github.kukpt.hydrological.common.ProtocolTrafficTopics;
import io.github.kukpt.sl427.buffer.Hex;
import io.github.kukpt.sl427.model.ProtocolMessage;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.NetSocket;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDate;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

public class Sl427ServerTest {

  private static final byte[] GOLDEN = Hex.decode(
    "681C68B33200100110C0AAAAAAAAAA050100000000002000000010300322001216");

  @Test
  public void tracksStationOnlineStateAndStoresTraffic() throws Exception {
    Vertx vertx = Vertx.vertx();
    Path storeDir = Files.createTempDirectory("sl427-traffic-store-");
    CompletableFuture<Sl427Endpoint> online = new CompletableFuture<>();
    CompletableFuture<Sl427Endpoint> offline = new CompletableFuture<>();
    CompletableFuture<ProtocolMessage> message = new CompletableFuture<>();
    Sl427Server server = Sl427Server.create(vertx, new Sl427ServerOptions()
        .setPort(0)
        .setTrafficLogBaseDir(storeDir.toString()))
      .endpointHandler(endpoint -> endpoint.messageHandler(message::complete))
      .onlineHandler(online::complete)
      .offlineHandler(offline::complete);

    NetSocket socket = null;
    try {
      server.listen().toCompletionStage().toCompletableFuture().get(5, TimeUnit.SECONDS);
      socket = vertx.createNetClient().connect(server.actualPort(), "127.0.0.1")
        .toCompletionStage().toCompletableFuture().get(5, TimeUnit.SECONDS);
      socket.write(Buffer.buffer(GOLDEN)).toCompletionStage().toCompletableFuture()
        .get(5, TimeUnit.SECONDS);

      Sl427Endpoint endpoint = online.get(5, TimeUnit.SECONDS);
      message.get(5, TimeUnit.SECONDS);
      String endpointId = "SL427:3200100110";
      assertEquals(endpointId, endpoint.endpointId());
      assertNotNull(endpoint.stationAddress());
      assertSame(endpoint, server.getEndpoint(endpointId));
      assertEquals(1, server.onlineEndpoints().size());

      JsonObject stored = readStoredTraffic(vertx, endpointId);
      JsonObject storedItem = inboundItem(stored.getJsonArray("items"));
      assertNotNull(storedItem);
      assertEquals("SL427", storedItem.getString("protocol"));
      assertEquals("SUCCESS", storedItem.getString("parseStatus"));
      assertEquals("RealtimeDataMessage", storedItem.getJsonObject("parsedData")
        .getString("messageType"));
      assertEquals("FLOW_TOTAL", storedItem.getJsonObject("parsedData")
        .getJsonObject("body").getString("parameterType"));

      socket.close().toCompletionStage().toCompletableFuture().get(5, TimeUnit.SECONDS);
      assertSame(endpoint, offline.get(5, TimeUnit.SECONDS));
      assertEquals(0, server.onlineEndpoints().size());
      socket = null;
    } finally {
      if (socket != null) socket.close();
      server.close().toCompletionStage().toCompletableFuture().get(5, TimeUnit.SECONDS);
      vertx.close().toCompletionStage().toCompletableFuture().get(5, TimeUnit.SECONDS);
    }
  }

  private static JsonObject inboundItem(JsonArray items) {
    for (int index = 0; index < items.size(); index++) {
      JsonObject item = items.getJsonObject(index);
      if ("INBOUND".equals(item.getString("type"))) return item;
    }
    return null;
  }

  @Test
  public void proxiesHttpOnSl427ProtocolPortToSharedMonitoringWeb() throws Exception {
    Vertx vertx = Vertx.vertx();
    Sl427Server server = Sl427Server.create(vertx, new Sl427ServerOptions()
        .setPort(0)
        .setEnableTrafficStore(false)
        .enableMetricsWeb(true)
        .enableHttpProxy(true)
        .setMetricsWebUserName("metrics-user")
        .setMetricsWebPassword("correct-password"))
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
      vertx.close().toCompletionStage().toCompletableFuture().get(5, TimeUnit.SECONDS);
    }
  }

  private static JsonObject readStoredTraffic(Vertx vertx, String endpointId) throws Exception {
    JsonObject request = new JsonObject()
      .put("endpointId", endpointId)
      .put("date", LocalDate.now().toString())
      .put("page", 1)
      .put("pageSize", 10);
    long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
    JsonObject result;
    do {
      Object body = vertx.eventBus().request(ProtocolTrafficTopics.READ, request)
        .toCompletionStage().toCompletableFuture().get(5, TimeUnit.SECONDS).body();
      result = (JsonObject) body;
      if (result.getInteger("total", 0) > 0) return result;
      Thread.sleep(20);
    } while (System.nanoTime() < deadline);
    return result;
  }
}
