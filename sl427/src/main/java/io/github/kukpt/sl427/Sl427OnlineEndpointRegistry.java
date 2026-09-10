package io.github.kukpt.sl427;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** Tracks identified SL427 stations that are currently online. */
public final class Sl427OnlineEndpointRegistry {

  private final ConcurrentMap<String, Entry> endpoints = new ConcurrentHashMap<>();

  public void register(Sl427Endpoint endpoint) {
    if (endpoint.stationAddress() == null) {
      throw new IllegalArgumentException("SL427 endpoint has not been identified");
    }
    Entry previous = endpoints.put(
      endpoint.endpointId(), new Entry(endpoint, LocalDateTime.now()));
    if (previous != null && previous.endpoint != endpoint) previous.endpoint.close();
  }

  public boolean unregister(Sl427Endpoint endpoint) {
    Entry current = endpoints.get(endpoint.endpointId());
    return current != null && current.endpoint == endpoint
      && endpoints.remove(endpoint.endpointId(), current);
  }

  public Sl427Endpoint get(String endpointId) {
    Entry entry = endpoints.get(endpointId);
    return entry == null ? null : entry.endpoint;
  }

  public Collection<Sl427Endpoint> endpoints() {
    List<Sl427Endpoint> result = new ArrayList<>();
    for (Entry entry : endpoints.values()) result.add(entry.endpoint);
    result.sort(Comparator.comparing(Sl427Endpoint::endpointId));
    return Collections.unmodifiableList(result);
  }

  public JsonArray toJson() {
    JsonArray result = new JsonArray();
    List<Entry> entries = new ArrayList<>(endpoints.values());
    entries.sort(Comparator.comparing(entry -> entry.endpoint.endpointId()));
    for (Entry entry : entries) {
      result.add(new JsonObject()
        .put("endpointId", entry.endpoint.endpointId())
        .put("stationAddress", entry.endpoint.stationAddress().canonicalKey())
        .put("remoteAddress", entry.endpoint.remoteAddress().toString())
        .put("connectedAt", entry.connectedAt.toString()));
    }
    return result;
  }

  private static final class Entry {
    private final Sl427Endpoint endpoint;
    private final LocalDateTime connectedAt;

    private Entry(Sl427Endpoint endpoint, LocalDateTime connectedAt) {
      this.endpoint = endpoint;
      this.connectedAt = connectedAt;
    }
  }
}
