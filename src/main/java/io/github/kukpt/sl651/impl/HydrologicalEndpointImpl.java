package io.github.kukpt.sl651.impl;

import com.google.common.collect.EvictingQueue;
import io.github.kukpt.sl651.HydrologicalEndpoint;
import io.github.kukpt.sl651.ReplyPromise;
import io.github.kukpt.sl651.codec.*;
import io.github.kukpt.sl651.metrics.MetricsStorage;
import io.github.kukpt.sl651.metrics.TrafficMonitor;
import io.github.kukpt.sl651.metrics.TrafficMonitorHandler;
import io.github.kukpt.sl651.utils.FrameEndType;
import io.netty.channel.ChannelPipeline;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.internal.net.NetSocketInternal;
import io.vertx.core.net.SocketAddress;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class HydrologicalEndpointImpl implements HydrologicalEndpoint {

  private boolean isDebug = false;

  private EvictingQueue<TrafficMonitor> queue;

  private final ReplyPromise reply = new ReplyPromise();

  private final AtomicInteger streamId = new AtomicInteger(0);

  private void setStreamId(int id) {
    synchronized (this.conn) {
      this.streamId.set(id);
    }
  }

  private int nextStreamId() {
    synchronized (this.conn) {
      return this.streamId.incrementAndGet();
    }
  }

  private final NetSocketInternal conn;

  private final String endpointId;

  private final int protocolPassword;

  private final short centralStationAddress;

  private Handler<UpstreamMessage> messageHandler;

  private Handler<Throwable> exceptionHandler;

  private Handler<HydrologicalEndpoint> closeHandler;

  private boolean isClosed;

  private final boolean isM2LinkMode;

  private FrameEndType frameEndType;

  private void initHandlers() {
    this.messageHandler = msg -> {
    };
  }

  public HydrologicalEndpointImpl(
  NetSocketInternal so,
  String endpointId,
  int protocolPassword,
  short centralStationAddress,
  boolean isM2LinkMode,
  FrameEndType frameEndType) {
    this.conn = so;
    this.endpointId = endpointId;
    this.protocolPassword = protocolPassword;
    this.centralStationAddress = centralStationAddress;
    this.isM2LinkMode = isM2LinkMode;
    this.frameEndType = frameEndType;
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
  public void enableDebug() {
    if (this.isDebug) {
      return;
    }
    this.isDebug = true;
    this.queue = EvictingQueue.<TrafficMonitor>create(24);
    MetricsStorage.me().setTrafficMonitorQueue(this, queue);
    ChannelPipeline pipeline = this.conn.channelHandlerContext().pipeline();
    pipeline.addFirst(new TrafficMonitorHandler(this, queue));
  }

  @Override
  public void disableDebug() {
    if (!this.isDebug) {
      return;
    }
    this.isDebug = false;
    ChannelPipeline pipeline = this.conn.channelHandlerContext().pipeline();
    pipeline.remove(TrafficMonitorHandler.class);
    this.queue = null;
    MetricsStorage.me().removeTrafficMonitorQueue(this);
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

  void handleMessage(UpstreamMessage msg) {
    synchronized (this.conn) {
      checkClosed();
      reply.setReply(msg);
      if (this.messageHandler != null) {
        this.messageHandler.handle(msg);
      }
      if (isM2LinkMode) {
        if (msg.header().isLinkKeep()) {
          return;
        }
        writeM2Ack(msg.header(), msg.streamId(), frameEndType);
      }
    }
  }

  @Override
  public HydrologicalEndpoint messageHandler(Handler<UpstreamMessage> messageHandler) {
    synchronized (this.conn) {
      checkClosed();
      this.messageHandler = messageHandler;
      return this;
    }
  }

  //  void handleTestMessage(HydrologicalMessage msg) {
//    synchronized (this.conn) {
//      this.checkClosed();
//      if (this.testMessageHandler != null) {
//        this.testMessageHandler.handle((TestMessage) msg.payload());
//      }
//    }
//  }
//
//  @Override
//  public HydrologicalEndpoint periodMessageHandler(Handler<PeriodMessage> periodMessageHandler) {
//    synchronized (this.conn) {
//      this.checkClosed();
//      this.periodMessageHandler = periodMessageHandler;
//      return this;
//    }
//  }
//
//  void handlePeriodMessage(HydrologicalMessage msg) {
//    synchronized (this.conn) {
//      this.checkClosed();
//      if (this.periodMessageHandler != null) {
//        this.periodMessageHandler.handle((PeriodMessage) msg.payload());
//      }
//    }
//  }
//
//  @Override
//  public HydrologicalEndpoint timingMessageHandler(Handler<TimingMessage> timingMessageHandler) {
//    synchronized (this.conn) {
//      this.checkClosed();
//      this.timingMessageHandler = timingMessageHandler;
//      return this;
//    }
//  }
//
//  void handleTimingMessage(HydrologicalMessage msg) {
//    synchronized (this.conn) {
//      this.checkClosed();
//      TimingMessage payload = (TimingMessage) msg.payload();
//      if (this.timingMessageHandler != null) {
//        this.timingMessageHandler.handle(payload);
//      }
//      if (isM2LinkMode()) {
//        this.writeM2Ack(msg.header(), payload.fixedBodyMessage().streamId());
//      }
//    }
//  }
//
//  @Override
//  public HydrologicalEndpoint additionalMessageHandler(Handler<AdditionalMessage> additionalMessageHandler) {
//    synchronized (this.conn) {
//      checkClosed();
//      this.additionalMessageHandler = additionalMessageHandler;
//      return this;
//    }
//  }
//
//  void handleAdditionalMessage(HydrologicalMessage msg) {
//    synchronized (this.conn) {
//      this.checkClosed();
//      if (this.additionalMessageHandler != null) {
//        this.additionalMessageHandler.handle((AdditionalMessage) msg.payload());
//      }
//    }
//  }
//
//  @Override
//  public HydrologicalEndpoint hourlyMessageHandler(Handler<HourlyMessage> hourlyMessageHandler) {
//    synchronized (this.conn) {
//      this.checkClosed();
//      this.hourlyMessageHandler = hourlyMessageHandler;
//      return this;
//    }
//  }
//
//  void handleHourlyMessage(HydrologicalMessage msg) {
//    synchronized (this.conn) {
//      this.checkClosed();
//      HourlyMessage payload = (HourlyMessage) msg.payload();
//      if (this.hourlyMessageHandler != null) {
//        this.hourlyMessageHandler.handle(payload);
//      }
//      if (isM2LinkMode()) {
//        this.writeM2Ack(msg.header(), payload.fixedBodyMessage().streamId());
//      }
//    }
//  }
//
//  @Override
//  public HydrologicalEndpoint pumpControlResponseHandler(Handler<PumpStationControlResponseMessage>
//  pumpControlResponseHandler) {
//    synchronized (this.conn) {
//      this.checkClosed();
//      this.pumpStationControlResponseMessageHandler = pumpControlResponseHandler;
//      return this;
//    }
//  }
//
//  void handlePumpStationControlResponseMessage(HydrologicalMessage msg) {
//    synchronized (this.conn) {
//      this.checkClosed();
//      PumpStationControlResponseMessage payload = (PumpStationControlResponseMessage) msg.payload();
//      if (this.pumpStationControlResponseMessageHandler != null) {
//        this.pumpStationControlResponseMessageHandler.handle(payload);
//      }
//      if (isM2LinkMode()) {
//        this.writeM2Ack(msg.header(), payload.streamId());
//      }
//    }
//  }

  @Override
  public HydrologicalEndpoint closeHandler(Handler<HydrologicalEndpoint> closeHandler) {
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

//  /**
//   * 泵站控制
//   * @param tsAddr
//   * @param command 按照对应的数据位，0 关， 1 开。目前总共可以控制8路，实际可以拓展。
//   * @return
//   */
//  @Override
//  public Future<Integer> pumpStationControl(String tsAddr, short command) {
//    Integer streamId = IdUtil.nextId();
//    PumpStationControlContent content = new PumpStationControlContent(streamId, ReportTime.now(), (short) 1, command);
//    return this.downstreamQueryControl(tsAddr, FunctionType.PUMP_CONTROL, content).map(streamId);
//  }
//
//  /**
//   * 下行查询控制
//   * @return
//   */
//  Future<Void> downstreamQueryControl(String tsAddr, FunctionType type, DownstreamMessageContent content) {
//    MessageHeader header = new MessageHeader(centralStationAddress, tsAddr, protocolPassword, type, 0, (byte)
//    HydroLogicalUtils.STX, 0, 0);
//    HydrologicalDownstreamMessage downstreamMessage = new HydrologicalDownstreamMessage(header, content,
//                                                                                        HydroLogicalUtils.ENQ);
//    return this.write(downstreamMessage);
//  }

  void handleException(Throwable t) {
    synchronized (this.conn) {
      this.checkClosed();
      if (this.exceptionHandler != null) {
        this.exceptionHandler.handle(t);
      }
    }
  }

  void handleClose(HydrologicalEndpoint endpoint) {
    synchronized (this.conn) {
      cleanUp();
      if (this.closeHandler != null) {
        this.closeHandler.handle(endpoint);
      }
    }
  }

  public Future<UpstreamMessage> request(DownstreamMessage dMsg, long timeout) {
    int ft = dMsg.functionType();
    return write(dMsg).compose(unused ->
                               reply.setPromise(ft).future().timeout(timeout, TimeUnit.SECONDS))
    .onFailure(t -> reply.onTimeOutClear(ft, t));
  }

  void writeM2Ack(MessageHeader header, int streamId, FrameEndType ft) {
    DownstreamMessage m2Ack = HydrologicalMessageFactory.createM2Ack(header, streamId, ft);
    this.write(m2Ack);
  }


  Future<Void> write(DownstreamMessage downstreamMessage) {
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

  @Override
  public int password() {
    return this.protocolPassword;
  }
}
