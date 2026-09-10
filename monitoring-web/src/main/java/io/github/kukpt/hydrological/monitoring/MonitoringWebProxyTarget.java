package io.github.kukpt.hydrological.monitoring;

import io.vertx.core.Vertx;
import io.vertx.core.shareddata.LocalMap;

/** Publishes the internal monitoring port to protocol-port HTTP proxies. */
public final class MonitoringWebProxyTarget {
  private static final String MAP_NAME = "hydrological.monitoring.web";
  private static final String PORT_KEY = "port";
  private static final String HOST_KEY = "host";

  private MonitoringWebProxyTarget() { }

  public static Integer port(Vertx vertx) {
    return (Integer) map(vertx).get(PORT_KEY);
  }

  public static String host(Vertx vertx) {
    return (String) map(vertx).get(HOST_KEY);
  }

  static void publish(Vertx vertx, String host, int port) {
    map(vertx).put(PORT_KEY, port);
    map(vertx).put(HOST_KEY,
      "0.0.0.0".equals(host) || "::".equals(host) ? "127.0.0.1" : host);
  }

  static void clear(Vertx vertx, int port) {
    map(vertx).removeIfPresent(PORT_KEY, port);
    map(vertx).remove(HOST_KEY);
  }

  private static LocalMap<String, Object> map(Vertx vertx) {
    return vertx.sharedData().getLocalMap(MAP_NAME);
  }
}
