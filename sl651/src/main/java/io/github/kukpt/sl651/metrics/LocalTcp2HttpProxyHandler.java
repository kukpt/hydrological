package io.github.kukpt.sl651.metrics;

import io.github.kukpt.hydrological.monitoring.LocalTcpToHttpProxyHandler;
import io.vertx.core.Vertx;
/** @deprecated use {@link LocalTcpToHttpProxyHandler}. */
@Deprecated
public class LocalTcp2HttpProxyHandler extends LocalTcpToHttpProxyHandler {
  public LocalTcp2HttpProxyHandler(Vertx vertx, int originPort) {
    super(vertx, originPort);
  }
}
