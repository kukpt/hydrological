package io.github.kukpt.sl651.impl;

import io.github.kukpt.sl651.Sl651Endpoint;
import io.github.kukpt.sl651.Sl651ServerOptions;
import io.github.kukpt.sl651.OnlineEndpointRegistry;
import io.github.kukpt.sl651.codec.UpstreamMessage;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.DecoderResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.net.impl.NetSocketInternal;


public class Sl651ServerConnection {

  private final Vertx vertx;

  private Handler<Sl651Endpoint> endpointHandler;

  private Handler<Throwable> exceptionHandler;

  private final NetSocketInternal so;

  private final ChannelHandlerContext chctx;

  private final Sl651ServerOptions options;

  private Sl651EndpointImpl endpoint;

  private final OnlineEndpointRegistry onlineEndpoints;

  public Sl651ServerConnection(
      Vertx vertx,
      NetSocketInternal so,
      Handler<Sl651Endpoint> endpointHandler,
      Handler<Throwable> exceptionHandler,
      Sl651ServerOptions options,
      OnlineEndpointRegistry onlineEndpoints) {
    this.vertx = vertx;
    this.so = so;
    this.chctx = so.channelHandlerContext();
    this.endpointHandler = endpointHandler;
    this.exceptionHandler = exceptionHandler;
    this.options = options;
    this.onlineEndpoints = onlineEndpoints;
  }


  void handleMsg(Object msg) {

    if (msg instanceof UpstreamMessage) {
      UpstreamMessage hydrologicalMessage = (UpstreamMessage) msg;
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

      handleMessage(hydrologicalMessage);

    }

  }

  void handleConnect(UpstreamMessage message) {
    if (this.endpoint != null) {
      return;
    }
    this.endpoint = new Sl651EndpointImpl(
        vertx,
        so,
        message.header().telemetryStationAddress(),
        message.header().password(),
        options.getCentralStationAddress(),
        options.isM2LinkMode(),
        options.getFrameEndType());
    this.onlineEndpoints.register(this.endpoint);
    this.endpointHandler.handle(this.endpoint);
    this.so.exceptionHandler(t -> this.endpoint.handleException(t));
    this.so.closeHandler(v -> {
      this.handleClose();
    });
  }

  void handleMessage(UpstreamMessage message) {
    synchronized (this.so) {
      this.checkEndpoint();
      this.endpoint.handleMessage(message);
    }
  }

  public void handleClose() {
    synchronized (this.so) {
      this.checkEndpoint();
      this.onlineEndpoints.unregister(this.endpoint);
      this.endpoint.handleClose(endpoint);
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
