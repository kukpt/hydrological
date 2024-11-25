package io.github.kukpt.sl651;

import io.github.kukpt.sl651.codec.*;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.net.SocketAddress;

public interface HydrologicalEndpoint {

  void close();

  SocketAddress remoteAddress();

  SocketAddress localAddress();

  HydrologicalEndpoint setM2LinkMode(boolean m2LinkMode);

  String endpointId();

  HydrologicalEndpoint testMessageHandler(Handler<TestMessage> testMessageHandler);

  HydrologicalEndpoint periodMessageHandler(Handler<PeriodMessage> periodMessageHandler);

  HydrologicalEndpoint timingMessageHandler(Handler<TimingMessage> timingMessageHandler);

  HydrologicalEndpoint additionalMessageHandler(Handler<AdditionalMessage> additionalMessageHandler);

  HydrologicalEndpoint hourlyMessageHandler(Handler<HourlyMessage> hourlyMessageHandler);

  HydrologicalEndpoint pumpControlResponseHandler(Handler<PumpStationControlResponseMessage> pumpControlResponseHandler);

  HydrologicalEndpoint closeHandler(Handler<Void> closeHandler);

  HydrologicalEndpoint exceptionHandler(Handler<Throwable> handler);
  /**
   * 泵站控制
   * @param tsAddr
   * @param command 按照对应的数据位，0 关， 1 开。目前总共可以控制8路，实际可以拓展。
   * @return
   */
  Future<Integer> pumpStationControl(String tsAddr, short command) ;
}
