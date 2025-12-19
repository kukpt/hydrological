package io.github.kukpt.sl651.metrics;

import io.github.kukpt.sl651.HydrologicalEndpoint;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;

public class MetricsStorage {

  private MetricsStorage(){}

  private static MetricsStorage me;

  public static MetricsStorage me() {
    if (me == null) {
      me = new MetricsStorage();
      return me;
    }
    return me;
  }

  static final ConcurrentHashMap<String, Endpoint> endpointMap = new ConcurrentHashMap<>();

  static final ConcurrentHashMap<String, Queue<TrafficMonitor>> trafficMonitorQueueMap = new ConcurrentHashMap<>();

  public void setTrafficMonitorQueue(HydrologicalEndpoint endpoint, Queue<TrafficMonitor> queue) {
    trafficMonitorQueueMap.put(endpoint.endpointId(), queue);
  }

  public void removeTrafficMonitorQueue(HydrologicalEndpoint endpoint) {
    trafficMonitorQueueMap.remove(endpoint.endpointId());
  }

  public List<TrafficMonitor> getTrafficMonitorQueueData(String endpointId) {
    Queue<TrafficMonitor> queue = trafficMonitorQueueMap.get(endpointId);
    if (queue != null) {
      return new ArrayList<>(queue);
    }
    return new ArrayList<>();
  }

  public void putEndpoint(HydrologicalEndpoint endpoint) {
    Endpoint e = new Endpoint(endpoint.endpointId(), endpoint.remoteAddress().toString(),
                                      Integer.toHexString(endpoint.password()));
    endpointMap.put(e.getEndpointId(), e);
    System.err.println(values());
  }
  public void removeEndPoint(HydrologicalEndpoint endpoint) {
    endpointMap.remove(endpoint.endpointId());
    System.err.println(values());
  }

  public ArrayList<Endpoint> values() {
    ArrayList<Endpoint> values = new ArrayList<>(endpointMap.size());
    values.addAll(endpointMap.values());
    return values;
  }

  public HashSet<String> keySet() {
    return new HashSet<>(endpointMap.keySet());
  }
}
