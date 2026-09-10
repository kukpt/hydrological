package io.github.kukpt.hydrological.common;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

public class ProtocolTrafficStoreVerticleTest {

  private Vertx vertx;
  private Path storeDir;
  private String deploymentId;

  @Before
  public void setUp() throws Exception {
    vertx = Vertx.vertx();
    storeDir = Files.createTempDirectory("endpoint-message-store-");
    deploymentId = ProtocolTrafficStoreVerticle.deployShared(vertx, storeDir.toString())
      .toCompletionStage().toCompletableFuture().get(10, TimeUnit.SECONDS);
  }

  @After
  public void tearDown() throws Exception {
    vertx.close().toCompletionStage().toCompletableFuture().get(10, TimeUnit.SECONDS);
  }

  @Test
  public void readsNewestMessagesByPage() throws Exception {
    for (int sequence = 1; sequence <= 5; sequence++) {
      JsonObject message = new JsonObject()
          .put("endpointId", "station-1")
          .put("sequence", sequence);
      vertx.eventBus().request(ProtocolTrafficTopics.APPEND, message)
          .toCompletionStage().toCompletableFuture().get(10, TimeUnit.SECONDS);
    }

    JsonObject request = new JsonObject()
        .put("endpointId", "station-1")
        .put("page", 2)
        .put("pageSize", 2);
    Object body = vertx.eventBus().request(ProtocolTrafficTopics.READ, request)
        .toCompletionStage().toCompletableFuture().get(10, TimeUnit.SECONDS).body();
    JsonObject result = (JsonObject) body;
    JsonArray items = result.getJsonArray("items");

    assertEquals(2, result.getInteger("page").intValue());
    assertEquals(2, result.getInteger("pageSize").intValue());
    assertEquals(5, result.getInteger("total").intValue());
    assertEquals(3, result.getInteger("totalPages").intValue());
    assertEquals(3, items.getJsonObject(0).getInteger("sequence").intValue());
    assertEquals(2, items.getJsonObject(1).getInteger("sequence").intValue());
  }

  @Test
  public void readsMessagesFromSelectedDate() throws Exception {
    JsonObject message = new JsonObject()
        .put("endpointId", "station-2")
        .put("recordedTime", "2026-07-20T08:30:00")
        .put("sequence", 20);
    vertx.eventBus().request(ProtocolTrafficTopics.APPEND, message)
        .toCompletionStage().toCompletableFuture().get(10, TimeUnit.SECONDS);

    JsonObject request = new JsonObject()
        .put("endpointId", "station-2")
        .put("date", "2026-07-20")
        .put("page", 1)
        .put("pageSize", 20);
    Object body = vertx.eventBus().request(ProtocolTrafficTopics.READ, request)
        .toCompletionStage().toCompletableFuture().get(10, TimeUnit.SECONDS).body();
    JsonObject result = (JsonObject) body;

    assertEquals("2026-07-20", result.getString("date"));
    assertEquals(1, result.getInteger("total").intValue());
    assertEquals(20, result.getJsonArray("items").getJsonObject(0)
        .getInteger("sequence").intValue());
  }

  @Test
  public void reusesStoreForSameVertxAndDirectory() throws Exception {
    String reused = ProtocolTrafficStoreVerticle.deployShared(vertx, storeDir.toString())
      .toCompletionStage().toCompletableFuture().get(10, TimeUnit.SECONDS);

    assertEquals(deploymentId, reused);
  }
}
