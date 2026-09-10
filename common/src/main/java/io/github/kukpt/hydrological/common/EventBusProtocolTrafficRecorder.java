package io.github.kukpt.hydrological.common;

import io.vertx.core.Vertx;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;

import java.util.Objects;

/** Writes traffic records to a Vert.x event-bus consumer such as the shared message store. */
public final class EventBusProtocolTrafficRecorder implements ProtocolTrafficRecorder {
  private static final Logger log = LoggerFactory.getLogger(EventBusProtocolTrafficRecorder.class);

  private final Vertx vertx;
  private final String address;

  public EventBusProtocolTrafficRecorder(Vertx vertx) {
    this(vertx, ProtocolTrafficTopics.APPEND);
  }

  public EventBusProtocolTrafficRecorder(Vertx vertx, String address) {
    this.vertx = Objects.requireNonNull(vertx, "vertx");
    this.address = Objects.requireNonNull(address, "address");
  }

  @Override
  public void record(ProtocolTrafficRecord record) {
    vertx.eventBus().request(address, record.toJson())
      .onFailure(error -> log.error("Failed to persist " + record.getProtocol() + " traffic record", error));
  }
}
