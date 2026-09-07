package io.github.kukpt.sl651.impl;

import io.github.kukpt.sl651.HydrologicalEndpoint;
import io.github.kukpt.sl651.ReplyPromise;
import io.github.kukpt.sl651.codec.*;
import io.github.kukpt.sl651.metrics.TrafficMonitorHandler;
import io.github.kukpt.sl651.utils.FrameEndType;
import io.github.kukpt.sl651.utils.HydroLogicalUtils;
import io.netty.channel.ChannelPipeline;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;

import io.vertx.core.Vertx;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.net.SocketAddress;
import io.vertx.core.net.impl.NetSocketInternal;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class HydrologicalEndpointImpl implements HydrologicalEndpoint {

  private final static Logger log = LoggerFactory.getLogger(HydrologicalEndpointImpl.class);

  private static final long DEFAULT_REQUEST_TIMEOUT = 30L;

  private boolean isDebug = false;

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

  private final Vertx vertx;

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
  Vertx vertx,
  NetSocketInternal so,
  String endpointId,
  int protocolPassword,
  short centralStationAddress,
  boolean isM2LinkMode,
  FrameEndType frameEndType) {
    this.vertx = vertx;
    this.conn = so;
    this.endpointId = endpointId;
    this.protocolPassword = protocolPassword;
    this.centralStationAddress = centralStationAddress;
    this.isM2LinkMode = isM2LinkMode;
    this.frameEndType = frameEndType;
    ChannelPipeline pipeline = this.conn.channelHandlerContext().pipeline();
    pipeline.addBefore("frame-decode", "traffic-monitor", new TrafficMonitorHandler(vertx, this));
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

  @Override
  public Future<PumpStationControlResponseMessage> pumpStationControl(short command, long timeout) {
    PumpStationControlContent content = new PumpStationControlContent(0, ReportTime.now(), (short) 1, command);
    return downstream(FunctionType.PUMP_CONTROL, content, HydroLogicalUtils.ENQ, timeout).map(upstreamMessage -> {
      final PumpStationControlResponseMessage[] response = new PumpStationControlResponseMessage[1];
      upstreamMessage.pumpStationControlResponseHandler(msg -> response[0] = msg);
      upstreamMessage.handle();
      if (response[0] == null) {
        throw new IllegalStateException("Pump station control response decode failed");
      }
      return response[0];
    });
  }

  @Override
  public Future<PumpStationControlResponseMessage> pumpStationControl(short command) {
    return pumpStationControl(command, DEFAULT_REQUEST_TIMEOUT);
  }

  @Override
  public Future<UpstreamMessage> downstream(
    int functionType,
    DownstreamMessagePayload payload,
    short frameControlType,
    long timeout) {
    return request(createDownstreamMessage(functionType, payload, frameControlType), timeout);
  }

  @Override
  public Future<UpstreamMessage> downstream(
    int functionType,
    DownstreamMessagePayload payload,
    long timeout) {
    return downstream(functionType, payload, HydroLogicalUtils.ENQ, timeout);
  }

  @Override
  public Future<UpstreamMessage> downstream(
    FunctionType functionType,
    DownstreamMessagePayload payload,
    short frameControlType,
    long timeout) {
    return downstream(functionType.value(), payload, frameControlType, timeout);
  }

  @Override
  public Future<UpstreamMessage> downstream(
    FunctionType functionType,
    DownstreamMessagePayload payload,
    long timeout) {
    return downstream(functionType, payload, HydroLogicalUtils.ENQ, timeout);
  }

  private DownstreamMessage createDownstreamMessage(
    int functionType,
    DownstreamMessagePayload content,
    short frameControlType) {
    MessageHeader header = new MessageHeader(
      centralStationAddress,
      endpointId,
      protocolPassword,
      functionType,
      0,
      HydroLogicalUtils.STX,
      0,
      0);
    return new DownstreamMessage(header, content, frameControlType);
  }

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
    Promise<UpstreamMessage> promise = reply.setPromise(dMsg);
    Future<UpstreamMessage> replyFuture = promise.future().timeout(timeout, TimeUnit.SECONDS);
    if (promise.future().failed()) {
      return promise.future();
    }
    write(dMsg).onFailure(t -> {
      reply.clear(dMsg, t);
      promise.tryFail(t);
    });
    return replyFuture.onFailure(t -> reply.clear(dMsg, t));
  }

  void writeM2Ack(MessageHeader header, int streamId, FrameEndType ft) {
    MessageHeader ackHeader = new MessageHeader(
      centralStationAddress,
      endpointId,
      protocolPassword,
      header.functionType(),
      0,
      HydroLogicalUtils.STX,
      0,
      0);
    DownstreamMessage m2Ack = HydrologicalMessageFactory.createM2Ack(ackHeader, streamId, ft);
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
