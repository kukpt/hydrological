package io.github.kukpt.sl651;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Tracks endpoints that are currently connected to this server instance.
 */
public class OnlineEndpointRegistry {

  private final ConcurrentMap<String, Entry> endpoints = new ConcurrentHashMap<>();

  public void register(HydrologicalEndpoint endpoint) {
    endpoints.put(endpoint.endpointId(), new Entry(endpoint, LocalDateTime.now()));
  }

  public void unregister(HydrologicalEndpoint endpoint) {
    endpoints.computeIfPresent(endpoint.endpointId(), (id, current) ->
      current.endpoint == endpoint ? null : current);
  }

  public HydrologicalEndpoint get(String endpointId) {
    Entry entry = endpoints.get(endpointId);
    return entry == null ? null : entry.endpoint;
  }

  public JsonArray toJson() {
    List<Entry> entries = new ArrayList<>(endpoints.values());
    entries.sort(Comparator.comparing(entry -> entry.endpoint.endpointId()));
    JsonArray result = new JsonArray();
    for (Entry entry : entries) {
      HydrologicalEndpoint endpoint = entry.endpoint;
      result.add(new JsonObject()
        .put("endpointId", endpoint.endpointId())
        .put("remoteAddress", endpoint.remoteAddress().toString())
        .put("connectedAt", entry.connectedAt.toString()));
    }
    return result;
  }

  private static final class Entry {
    private final HydrologicalEndpoint endpoint;
    private final LocalDateTime connectedAt;

    private Entry(HydrologicalEndpoint endpoint, LocalDateTime connectedAt) {
      this.endpoint = endpoint;
      this.connectedAt = connectedAt;
    }
  }
}
