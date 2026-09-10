package io.github.kukpt.hydrological.common;

/** Shared event-bus addresses for protocol traffic storage. */
public final class ProtocolTrafficTopics {
  public static final String APPEND = "hydrological.endpoint.message.append";
  public static final String READ = "hydrological.endpoint.message.read";
  public static final String LIST = "hydrological.endpoint.message.list";

  private ProtocolTrafficTopics() { }
}
