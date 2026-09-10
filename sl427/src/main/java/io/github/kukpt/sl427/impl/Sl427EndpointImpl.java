package io.github.kukpt.sl427.impl;

import io.github.kukpt.hydrological.common.ProtocolDecodeLogger;
import io.github.kukpt.hydrological.common.ProtocolTrafficRecord;
import io.github.kukpt.hydrological.common.ProtocolTrafficRecorder;
import io.github.kukpt.hydrological.common.ProtocolType;
import io.github.kukpt.hydrological.common.TrafficDirection;
import io.github.kukpt.sl427.Sl427Endpoint;
import io.github.kukpt.sl427.Sl427Parser;
import io.github.kukpt.sl427.Sl427ServerOptions;
import io.github.kukpt.sl427.codec.Sl427S2ConfirmationEncoder;
import io.github.kukpt.sl427.frame.StationAddress;
import io.github.kukpt.sl427.model.ProtocolMessage;
import io.github.kukpt.sl427.result.ParseResult;
import io.github.kukpt.sl427.result.ParseStatus;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.net.NetSocket;
import io.vertx.core.net.SocketAddress;

/** Per-connection SL427 endpoint implementation. */
public final class Sl427EndpointImpl implements Sl427Endpoint {

  private static final Logger log = LoggerFactory.getLogger(Sl427EndpointImpl.class);

  private final NetSocket socket;
  private final Sl427ServerOptions options;
  private final String remoteAddress;
  private final Sl427Parser parser;
  private final ProtocolTrafficRecorder trafficRecorder;
  private final Handler<Sl427Endpoint> identifiedHandler;
  private final Handler<Sl427Endpoint> disconnectedHandler;
  private final Sl427S2ConfirmationEncoder confirmationEncoder = new Sl427S2ConfirmationEncoder();

  private Handler<ProtocolMessage> messageHandler;
  private Handler<ParseResult<ProtocolMessage>> decodeFailureHandler;
  private Handler<Sl427Endpoint> closeHandler;
  private Handler<Throwable> exceptionHandler;
  private volatile String endpointId;
  private volatile StationAddress stationAddress;
  private boolean closed;

  Sl427EndpointImpl(NetSocket socket, Sl427ServerOptions options,
      ProtocolTrafficRecorder trafficRecorder, Handler<Sl427Endpoint> identifiedHandler,
      Handler<Sl427Endpoint> disconnectedHandler) {
    this.socket = socket;
    this.options = options;
    this.remoteAddress = socket.remoteAddress().toString();
    this.endpointId = "SL427:connection:" + remoteAddress;
    this.trafficRecorder = trafficRecorder;
    this.identifiedHandler = identifiedHandler;
    this.disconnectedHandler = disconnectedHandler;
    this.parser = new Sl427Parser(Sl427Endpoint::endpointId, endpointId,
      remoteAddress, trafficRecorder, options.getAddressMode(),
      options.isIncludeParsedTrafficData());
  }

  void start() {
    socket.exceptionHandler(this::handleException);
    socket.closeHandler(unused -> handleClose());
    socket.handler(buffer -> {
      try {
        for (ParseResult<ProtocolMessage> result : parser.accept(
          buffer, options.getProtocolContextResolver(), options.getProtocolContext())) {
          handleResult(result);
        }
      } catch (RuntimeException error) {
        parser.resetStream();
        handleException(error);
      }
    });
  }

  @Override
  public void close() {
    socket.close();
  }

  @Override
  public SocketAddress remoteAddress() {
    return socket.remoteAddress();
  }

  @Override
  public SocketAddress localAddress() {
    return socket.localAddress();
  }

  @Override
  public String endpointId() {
    return endpointId;
  }

  @Override
  public StationAddress stationAddress() {
    return stationAddress;
  }

  @Override
  public synchronized Sl427Endpoint messageHandler(Handler<ProtocolMessage> handler) {
    messageHandler = handler;
    return this;
  }

  @Override
  public synchronized Sl427Endpoint decodeFailureHandler(
      Handler<ParseResult<ProtocolMessage>> handler) {
    decodeFailureHandler = handler;
    return this;
  }

  @Override
  public synchronized Sl427Endpoint closeHandler(Handler<Sl427Endpoint> handler) {
    closeHandler = handler;
    return this;
  }

  @Override
  public synchronized Sl427Endpoint exceptionHandler(Handler<Throwable> handler) {
    exceptionHandler = handler;
    return this;
  }

  private void handleResult(ParseResult<ProtocolMessage> result) {
    if (result.isSuccess()) {
      ProtocolMessage message = result.value();
      if (!identify(message.frame().address())) return;
      Handler<ProtocolMessage> handler = messageHandler;
      if (handler != null) invoke(() -> handler.handle(message));
      if (options.isAutoConfirmation() && confirmationEncoder.supports(message.frame())) {
        sendS2Confirmation(message);
      }
    } else if (result.status() != ParseStatus.PARTIAL) {
      Handler<ParseResult<ProtocolMessage>> handler = decodeFailureHandler;
      if (handler != null) invoke(() -> handler.handle(result));
    }
  }

  private void sendS2Confirmation(ProtocolMessage message) {
    byte[] confirmation = confirmationEncoder.encode(
      message.frame(), options.getConfirmationWorkMode());
    try {
      trafficRecorder.record(new ProtocolTrafficRecord(
        ProtocolType.SL427, TrafficDirection.OUTBOUND, endpointId, remoteAddress,
        ProtocolDecodeLogger.fullHex(confirmation)));
    } catch (RuntimeException error) {
      handleException(error);
    }
    socket.write(Buffer.buffer(confirmation)).onFailure(this::handleException);
  }

  private void handleClose() {
    Handler<Sl427Endpoint> handler;
    synchronized (this) {
      if (closed) return;
      closed = true;
      handler = closeHandler;
    }
    if (stationAddress != null && disconnectedHandler != null) {
      invoke(() -> disconnectedHandler.handle(this));
    }
    if (handler != null) invoke(() -> handler.handle(this));
  }

  private boolean identify(StationAddress address) {
    boolean identified = false;
    synchronized (this) {
      if (stationAddress == null) {
        stationAddress = address;
        endpointId = Sl427Endpoint.endpointId(address);
        identified = true;
      } else if (!stationAddress.canonicalKey().equals(address.canonicalKey())) {
        handleException(new IllegalStateException(
          "SL427 connection changed station address from " + stationAddress + " to " + address));
        socket.close();
        return false;
      }
    }
    if (identified && identifiedHandler != null) {
      Handler<Sl427Endpoint> handler = identifiedHandler;
      invoke(() -> handler.handle(this));
    }
    return true;
  }

  private void invoke(Runnable callback) {
    try {
      callback.run();
    } catch (RuntimeException error) {
      handleException(error);
    }
  }

  private void handleException(Throwable error) {
    Handler<Throwable> handler = exceptionHandler;
    if (handler != null) {
      try {
        handler.handle(error);
        return;
      } catch (RuntimeException callbackError) {
        log.error("SL427 endpoint exception handler failed, remote=" + remoteAddress, callbackError);
      }
    }
    log.error("SL427 endpoint error, remote=" + remoteAddress, error);
  }
}
