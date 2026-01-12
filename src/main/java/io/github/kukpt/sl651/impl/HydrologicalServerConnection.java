package io.github.kukpt.sl651.impl;

import io.github.kukpt.sl651.HydrologicalEndpoint;
import io.github.kukpt.sl651.HydrologicalServerOptions;
import io.github.kukpt.sl651.codec.UpstreamMessage;
import io.github.kukpt.sl651.metrics.MetricsStorage;
import io.github.kukpt.sl651.utils.LocalEbTopic;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.DecoderResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.net.impl.NetSocketInternal;


public class HydrologicalServerConnection {

  private final Vertx vertx;

  private Handler<HydrologicalEndpoint> endpointHandler;

  private Handler<Throwable> exceptionHandler;

  private final NetSocketInternal so;

  private final ChannelHandlerContext chctx;

  private final HydrologicalServerOptions options;

  private HydrologicalEndpointImpl endpoint;

  private MessageConsumer<Object> enableDebug;

  private MessageConsumer<Object> disableDebug;

  private void consumeEbMsg() {
    this.enableDebug = vertx.eventBus()
                            .localConsumer(LocalEbTopic.generateEnableEndpointDebugTopic(this.endpoint.endpointId()), unused -> this.endpoint.enableDebug());
    this.disableDebug = vertx.eventBus()
                             .localConsumer(LocalEbTopic.generateDisableEndpointDebugTopic(this.endpoint.endpointId()), unused -> this.endpoint.disableDebug());
  }

  public HydrologicalServerConnection(
      Vertx vertx,
      NetSocketInternal so,
      Handler<HydrologicalEndpoint> endpointHandler,
      Handler<Throwable> exceptionHandler,
      HydrologicalServerOptions options) {
    this.vertx = vertx;
    this.so = so;
    this.chctx = so.channelHandlerContext();
    this.endpointHandler = endpointHandler;
    this.exceptionHandler = exceptionHandler;
    this.options = options;
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
    this.endpoint = new HydrologicalEndpointImpl(so,
        message.header().telemetryStationAddress(),
        message.header().password(),
        options.getCentralStationAddress(),
        options.isM2LinkMode(),
        options.getFrameEndType());
    MetricsStorage.me().putEndpoint(this.endpoint);
    this.consumeEbMsg();
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
      MetricsStorage.me().removeEndPoint(endpoint);
      this.enableDebug.unregister();
      this.disableDebug.unregister();
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
