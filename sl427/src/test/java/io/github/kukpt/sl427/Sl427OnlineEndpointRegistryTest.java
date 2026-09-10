package io.github.kukpt.sl427;

import io.github.kukpt.sl427.buffer.Hex;
import io.github.kukpt.sl427.frame.StationAddress;
import io.github.kukpt.sl427.model.ProtocolMessage;
import io.github.kukpt.sl427.result.ParseResult;
import io.vertx.core.Handler;
import io.vertx.core.net.SocketAddress;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class Sl427OnlineEndpointRegistryTest {

  @Test
  public void newerConnectionReplacesOlderConnectionForSameStation() {
    Sl427OnlineEndpointRegistry registry = new Sl427OnlineEndpointRegistry();
    StubEndpoint oldEndpoint = new StubEndpoint();
    StubEndpoint newEndpoint = new StubEndpoint();

    registry.register(oldEndpoint);
    registry.register(newEndpoint);

    assertTrue(oldEndpoint.closed);
    assertSame(newEndpoint, registry.get(newEndpoint.endpointId()));
    assertFalse(registry.unregister(oldEndpoint));
    assertEquals(1, registry.endpoints().size());
    assertTrue(registry.unregister(newEndpoint));
    assertTrue(registry.endpoints().isEmpty());
  }

  private static final class StubEndpoint implements Sl427Endpoint {
    private final StationAddress address = StationAddress.parse(Hex.decode("3200100110"));
    private boolean closed;

    @Override public void close() { closed = true; }
    @Override public SocketAddress remoteAddress() { return SocketAddress.inetSocketAddress(1000, "127.0.0.1"); }
    @Override public SocketAddress localAddress() { return SocketAddress.inetSocketAddress(11824, "127.0.0.1"); }
    @Override public String endpointId() { return Sl427Endpoint.endpointId(address); }
    @Override public StationAddress stationAddress() { return address; }
    @Override public Sl427Endpoint messageHandler(Handler<ProtocolMessage> handler) { return this; }
    @Override public Sl427Endpoint decodeFailureHandler(Handler<ParseResult<ProtocolMessage>> handler) { return this; }
    @Override public Sl427Endpoint closeHandler(Handler<Sl427Endpoint> handler) { return this; }
    @Override public Sl427Endpoint exceptionHandler(Handler<Throwable> handler) { return this; }
  }
}
