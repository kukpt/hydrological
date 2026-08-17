package io.github.kukpt.sl651;

import io.github.kukpt.sl651.codec.DownstreamMessage;
import io.github.kukpt.sl651.codec.DownstreamMessagePayload;
import io.github.kukpt.sl651.codec.FunctionType;
import io.github.kukpt.sl651.codec.PumpStationControlResponseMessage;
import io.github.kukpt.sl651.codec.UpstreamMessage;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.net.SocketAddress;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

public class OnlineEndpointRegistryTest {

  @Test
  public void keepsReplacementWhenOldConnectionCloses() {
    OnlineEndpointRegistry registry = new OnlineEndpointRegistry();
    StubEndpoint oldConnection = new StubEndpoint("station-1", 1001);
    StubEndpoint newConnection = new StubEndpoint("station-1", 1002);

    registry.register(oldConnection);
    registry.register(newConnection);
    registry.unregister(oldConnection);

    assertSame(newConnection, registry.get("station-1"));
    assertEquals(1, registry.toJson().size());
    assertEquals("station-1", registry.toJson().getJsonObject(0).getString("endpointId"));

    registry.unregister(newConnection);
    assertNull(registry.get("station-1"));
  }

  private static final class StubEndpoint implements HydrologicalEndpoint {
    private final String id;
    private final int port;

    private StubEndpoint(String id, int port) { this.id = id; this.port = port; }
    @Override public void close() {}
    @Override public SocketAddress remoteAddress() { return SocketAddress.inetSocketAddress(port, "127.0.0.1"); }
    @Override public SocketAddress localAddress() { return SocketAddress.inetSocketAddress(11883, "127.0.0.1"); }
    @Override public String endpointId() { return id; }
    @Override public int password() { return 0; }
    @Override public Future<UpstreamMessage> request(DownstreamMessage dMsg, long timeout) { return Future.failedFuture("unused"); }
    @Override public Future<UpstreamMessage> downstream(int type, DownstreamMessagePayload payload, short control, long timeout) { return Future.failedFuture("unused"); }
    @Override public Future<UpstreamMessage> downstream(int type, DownstreamMessagePayload payload, long timeout) { return Future.failedFuture("unused"); }
    @Override public Future<UpstreamMessage> downstream(FunctionType type, DownstreamMessagePayload payload, short control, long timeout) { return Future.failedFuture("unused"); }
    @Override public Future<UpstreamMessage> downstream(FunctionType type, DownstreamMessagePayload payload, long timeout) { return Future.failedFuture("unused"); }
    @Override public Future<PumpStationControlResponseMessage> pumpStationControl(short command, long timeout) { return Future.failedFuture("unused"); }
    @Override public Future<PumpStationControlResponseMessage> pumpStationControl(short command) { return Future.failedFuture("unused"); }
    @Override public HydrologicalEndpoint messageHandler(Handler<UpstreamMessage> handler) { return this; }
    @Override public HydrologicalEndpoint closeHandler(Handler<HydrologicalEndpoint> handler) { return this; }
    @Override public HydrologicalEndpoint exceptionHandler(Handler<Throwable> handler) { return this; }
  }
}
