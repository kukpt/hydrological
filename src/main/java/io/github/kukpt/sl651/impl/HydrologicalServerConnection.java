package io.github.kukpt.sl651.impl;

import io.github.kukpt.sl651.HydrologicalEndpoint;
import io.github.kukpt.sl651.HydrologicalServerOptions;
import io.github.kukpt.sl651.codec.HydrologicalMessage;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.DecoderResult;
import io.vertx.core.Handler;
import io.vertx.core.net.impl.NetSocketInternal;

public class HydrologicalServerConnection {

  private Handler<HydrologicalEndpoint> endpointHandler;

  private Handler<Throwable> exceptionHandler;

  private final NetSocketInternal so;

  private final ChannelHandlerContext chctx;

  private final HydrologicalServerOptions options;

  private HydrologicalEndpointImpl endpoint;

  public HydrologicalServerConnection(NetSocketInternal so,
                                      Handler<HydrologicalEndpoint> endpointHandler,
                                      Handler<Throwable> exceptionHandler,
                                      HydrologicalServerOptions options) {
    this.so = so;
    this.chctx = so.channelHandlerContext();
    this.endpointHandler = endpointHandler;
    this.exceptionHandler = exceptionHandler;
    this.options = options;
  }


  void handleMsg(Object msg) {

    if (msg instanceof HydrologicalMessage) {
      HydrologicalMessage hydrologicalMessage = (HydrologicalMessage) msg;
      DecoderResult decoderResult = hydrologicalMessage.coderResult();
      if (decoderResult.isFailure()) {
        chctx.pipeline().fireExceptionCaught(decoderResult.cause());
        return;
      }
      if (!decoderResult.isFinished()) {
        chctx.pipeline().fireExceptionCaught(new Exception("Unfinished message"));
        return;
      }

      handleConnect(hydrologicalMessage);

      switch (hydrologicalMessage.header().functionType()) {
        case LINK_KEEP:
          break;
        case TEST:
          this.handleTestMessage(hydrologicalMessage);
          break;
        case PERIOD:
          this.handlePeriodMessage(hydrologicalMessage);
          break;
        case TIMING:
          this.handlerTimingMessage(hydrologicalMessage);
          break;
        case ADDITIONAL:
          this.handleAdditionalMessage(hydrologicalMessage);
          break;
        case HOURLY:
          this.handleHourlyMessage(hydrologicalMessage);
          break;
        case PUMP_CONTROL:
          this.handlePumpControl(hydrologicalMessage);
          break;
        default:
          this.chctx.fireExceptionCaught(new Exception("Wrong message function type " + hydrologicalMessage.header().functionType()));
          break;
      }

    }

  }

  void handleConnect(HydrologicalMessage message) {
    if (endpoint != null) {
      return;
    }
    this.endpoint = new HydrologicalEndpointImpl(so, message.header().telemetryStationAddress(),
                                                 options.getProtocolPassword(), options.getCentralStationAddress());
    this.endpointHandler.handle(this.endpoint);
    this.so.exceptionHandler(t -> this.endpoint.handleException(t));
    this.so.closeHandler(v -> this.endpoint.handleClose());
  }

  void handlePumpControl(HydrologicalMessage message) {
    synchronized (this.so) {
      this.checkEndpoint();
      this.endpoint.handlePumpStationControlResponseMessage(message);
    }
  }

  void handleTestMessage(HydrologicalMessage message) {
    synchronized (this.so) {
      this.checkEndpoint();
      this.endpoint.handleTestMessage(message);
    }
  }

  void handlePeriodMessage(HydrologicalMessage message) {
    synchronized (this.so) {
      this.checkEndpoint();
      this.endpoint.handlePeriodMessage(message);
    }
  }

  void handlerTimingMessage(HydrologicalMessage message) {
    synchronized (this.so) {
      this.checkEndpoint();
      this.endpoint.handleTimingMessage(message);
    }
  }

  void handleAdditionalMessage(HydrologicalMessage message) {
    synchronized (this.so) {
      this.checkEndpoint();
      this.endpoint.handleAdditionalMessage(message);
    }
  }

  void handleHourlyMessage(HydrologicalMessage message) {
    synchronized (this.so) {
      this.checkEndpoint();
      this.endpoint.handleHourlyMessage(message);
    }
  }

  private boolean checkEndpoint() {
    synchronized (this.so) {
      if (this.endpoint != null) {
        return true;
      } else {
        so.close();
        throw new IllegalStateException();
      }
    }
  }

}
