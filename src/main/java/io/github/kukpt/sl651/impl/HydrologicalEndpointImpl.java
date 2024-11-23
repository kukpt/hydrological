package io.github.kukpt.sl651.impl;

import io.github.kukpt.sl651.HydrologicalEndpoint;
import io.github.kukpt.sl651.codec.*;
import io.github.kukpt.sl651.utils.HydroLogicalUtils;
import io.github.kukpt.sl651.utils.IdUtil;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.net.SocketAddress;
import io.vertx.core.net.impl.NetSocketInternal;

public class HydrologicalEndpointImpl implements HydrologicalEndpoint {

  private final NetSocketInternal conn;

  private final String endpointId;

  private final int protocolPassword;

  private final short centralStationAddress;

  private Handler<Throwable> exceptionHandler;

  private Handler<Void> closeHandler;

  private Handler<TestMessage> testMessageHandler;

  private Handler<PeriodMessage> periodMessageHandler;

  private Handler<TimingMessage> timingMessageHandler;

  private Handler<AdditionalMessage> additionalMessageHandler;

  private Handler<HourlyMessage> hourlyMessageHandler;

  private Handler<PumpStationControlResponseMessage> pumpStationControlResponseMessageHandler;

  private boolean isClosed;

  private boolean isM2LinkMode = true;

  public HydrologicalEndpointImpl(NetSocketInternal so, String endpointId, int protocolPassword, short centralStationAddress) {
    this.conn = so;
    this.endpointId = endpointId;
    this.protocolPassword = protocolPassword;
    this.centralStationAddress = centralStationAddress;
  }

  @Override
  public void close() {
    synchronized (this.conn) {
      checkClosed();
      this.conn.close();
      this.cleanUp();
    }
  }

  @Override
  public SocketAddress remoteAddress() {
    synchronized (this.conn) {
      checkClosed();
      return this.conn.remoteAddress();
    }
  }

  @Override
  public SocketAddress localAddress() {
    synchronized (this.conn) {
      checkClosed();
      return this.conn.localAddress();
    }
  }

  @Override
  public HydrologicalEndpoint setM2LinkMode(boolean m2LinkMode) {
    this.isM2LinkMode = m2LinkMode;
    return this;
  }

  private boolean isM2LinkMode() {
    return isM2LinkMode;
  }

  @Override
  public HydrologicalEndpoint testMessageHandler(Handler<TestMessage> testMessageHandler) {
    synchronized (this.conn) {
      checkClosed();
      this.testMessageHandler = testMessageHandler;
      return this;
    }
  }

  void handleTestMessage(HydrologicalMessage msg) {
    synchronized (this.conn) {
      this.checkClosed();
      if (this.testMessageHandler != null) {
        this.testMessageHandler.handle((TestMessage) msg.payload());
      }
    }
  }

  @Override
  public HydrologicalEndpoint periodMessageHandler(Handler<PeriodMessage> periodMessageHandler) {
    synchronized (this.conn) {
      this.checkClosed();
      this.periodMessageHandler = periodMessageHandler;
      return this;
    }
  }

  void handlePeriodMessage(HydrologicalMessage msg) {
    synchronized (this.conn) {
      this.checkClosed();
      if (this.periodMessageHandler != null) {
        this.periodMessageHandler.handle((PeriodMessage) msg.payload());
      }
    }
  }

  @Override
  public HydrologicalEndpoint timingMessageHandler(Handler<TimingMessage> timingMessageHandler) {
    synchronized (this.conn) {
      this.checkClosed();
      this.timingMessageHandler = timingMessageHandler;
      return this;
    }
  }

  void handleTimingMessage(HydrologicalMessage msg) {
    synchronized (this.conn) {
      this.checkClosed();
      TimingMessage payload = (TimingMessage) msg.payload();
      if (this.timingMessageHandler != null) {
        this.timingMessageHandler.handle(payload);
      }
      if (isM2LinkMode()) {
        this.writeM2Ack(msg.header(), payload.fixedBodyMessage().streamId());
      }
    }
  }

  @Override
  public HydrologicalEndpoint additionalMessageHandler(Handler<AdditionalMessage> additionalMessageHandler) {
    synchronized (this.conn) {
      checkClosed();
      this.additionalMessageHandler = additionalMessageHandler;
      return this;
    }
  }

  void handleAdditionalMessage(HydrologicalMessage msg) {
    synchronized (this.conn) {
      this.checkClosed();
      if (this.additionalMessageHandler != null) {
        this.additionalMessageHandler.handle((AdditionalMessage) msg.payload());
      }
    }
  }

  @Override
  public HydrologicalEndpoint hourlyMessageHandler(Handler<HourlyMessage> hourlyMessageHandler) {
    synchronized (this.conn) {
      this.checkClosed();
      this.hourlyMessageHandler = hourlyMessageHandler;
      return this;
    }
  }

  void handleHourlyMessage(HydrologicalMessage msg) {
    synchronized (this.conn) {
      this.checkClosed();
      HourlyMessage payload = (HourlyMessage) msg.payload();
      if (this.hourlyMessageHandler != null) {
        this.hourlyMessageHandler.handle(payload);
      }
      if (isM2LinkMode()) {
        this.writeM2Ack(msg.header(), payload.fixedBodyMessage().streamId());
      }
    }
  }

  @Override
  public HydrologicalEndpoint pumpControlResponseHandler(Handler<PumpStationControlResponseMessage> pumpControlResponseHandler) {
    synchronized (this.conn) {
      this.checkClosed();
      this.pumpStationControlResponseMessageHandler = pumpControlResponseHandler;
      return this;
    }
  }

  void handlePumpStationControlResponseMessage(HydrologicalMessage msg) {
    synchronized (this.conn) {
      this.checkClosed();
      PumpStationControlResponseMessage payload = (PumpStationControlResponseMessage) msg.payload();
      if (this.pumpStationControlResponseMessageHandler != null) {
        this.pumpStationControlResponseMessageHandler.handle(payload);
      }
      if (isM2LinkMode()) {
        this.writeM2Ack(msg.header(), payload.streamId());
      }
    }
  }

  @Override
  public HydrologicalEndpoint closeHandler(Handler<Void> closeHandler) {
    synchronized (this.conn) {
      this.closeHandler = closeHandler;
    }
    return this;
  }

  @Override
  public HydrologicalEndpoint exceptionHandler(Handler<Throwable> handler) {
    synchronized (this.conn) {
      this.checkClosed();
      this.exceptionHandler = handler;
      return this;
    }
  }

  /**
   * 泵站控制
   * @param tsAddr
   * @param command 按照对应的数据位，0 关， 1 开。目前总共可以控制8路，实际可以拓展。
   * @return
   */
  @Override
  public Future<Integer> pumpStationControl(String tsAddr, short command) {
    Integer streamId = IdUtil.nextId();
    PumpStationControlContent content = new PumpStationControlContent(streamId, ReportTime.now(), (short) 1, command);
    return this.downstreamQueryControl(tsAddr, FunctionType.PUMP_CONTROL, content).map(streamId);
  }

  /**
   * 下行查询控制
   * @return
   */
  Future<Void> downstreamQueryControl(String tsAddr, FunctionType type, DownstreamMessageContent content) {
    MessageHeader header = new MessageHeader(centralStationAddress, tsAddr, protocolPassword, type, 0);
    HydrologicalDownstreamMessage downstreamMessage = new HydrologicalDownstreamMessage(header, content,
                                                                                        HydroLogicalUtils.ENQ);
    return this.write(downstreamMessage);
  }

  void handleException(Throwable t) {
    synchronized (this.conn) {
      this.checkClosed();
      if (this.exceptionHandler != null) {
        this.exceptionHandler.handle(t);
      }
    }
  }

  void handleClose() {
    synchronized (this.conn) {
      cleanUp();
      if (this.closeHandler != null) {
        this.closeHandler.handle(null);
      }
    }
  }

  Future<Void> writeM2Ack(MessageHeader header, int streamId) {
    HydrologicalDownstreamMessage m2Ack = HydrologicalMessageFactory.createM2Ack(header, streamId);
    return this.write(m2Ack);
  }


  Future<Void> write(HydrologicalDownstreamMessage downstreamMessage) {
    synchronized (this.conn) {
      this.checkClosed();
      return this.conn.writeMessage(downstreamMessage);
    }
  }


  void cleanUp() {
    this.isClosed = true;
  }

  private void checkClosed() {
    if (this.isClosed) {
      throw new IllegalStateException("Hydrological endpoint is closed");
    }
  }

  public String endpointId() {
    return this.endpointId;
  }
}
