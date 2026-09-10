package io.github.kukpt.sl427;

import io.github.kukpt.sl427.model.ProtocolMessage;
import io.github.kukpt.sl427.result.ParseResult;
import io.github.kukpt.sl427.frame.StationAddress;
import io.vertx.core.Handler;
import io.vertx.core.net.SocketAddress;

/** A connected SL427 terminal. */
public interface Sl427Endpoint {

  static String endpointId(StationAddress address) {
    if (address == null) throw new IllegalArgumentException("address must not be null");
    return "SL427:" + address.canonicalKey();
  }

  void close();

  SocketAddress remoteAddress();

  SocketAddress localAddress();

  String endpointId();

  /** Returns the station address after the first valid message, otherwise {@code null}. */
  StationAddress stationAddress();

  Sl427Endpoint messageHandler(Handler<ProtocolMessage> messageHandler);

  Sl427Endpoint decodeFailureHandler(Handler<ParseResult<ProtocolMessage>> decodeFailureHandler);

  Sl427Endpoint closeHandler(Handler<Sl427Endpoint> closeHandler);

  Sl427Endpoint exceptionHandler(Handler<Throwable> exceptionHandler);
}
