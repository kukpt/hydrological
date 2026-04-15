package io.github.kukpt.sl651;

import io.github.kukpt.sl651.codec.*;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.net.SocketAddress;

public interface HydrologicalEndpoint {

  void close();

  SocketAddress remoteAddress();

  SocketAddress localAddress();

  String endpointId();

  int password();

  Future<UpstreamMessage> request(DownstreamMessage dMsg, long timeout);

  HydrologicalEndpoint messageHandler(Handler<UpstreamMessage> messageHandler);

  HydrologicalEndpoint closeHandler(Handler<HydrologicalEndpoint> closeHandler);

  HydrologicalEndpoint exceptionHandler(Handler<Throwable> handler);
//  /**
//   * 泵站控制
//   * @param tsAddr
//   * @param command 按照对应的数据位，0 关， 1 开。目前总共可以控制8路，实际可以拓展。
//   * @return
//   */
//  Future<Integer> pumpStationControl(String tsAddr, short command) ;
}
