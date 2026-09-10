package io.github.kukpt.sl651.utils;

import io.github.kukpt.hydrological.common.ProtocolTrafficTopics;

public final class LocalEbTopic {
  private LocalEbTopic() {}
  private static String ENABLE_ENDPOINT_DEBUG = "hydrological.endpoint.debug.enable.%s";

  private static String DISABLE_ENDPOINT_DEBUG = "hydrological.endpoint.debug.disable.%s";

  public static String generateEnableEndpointDebugTopic(String endpointId) {
    return String.format(ENABLE_ENDPOINT_DEBUG, endpointId);
  }

  public static String generateDisableEndpointDebugTopic(String endpointId) {
    return String.format(DISABLE_ENDPOINT_DEBUG, endpointId);
  }


  public static String endpointMessageAppendTopic() {
    return ProtocolTrafficTopics.APPEND;
  }

  public static String endpointMessageReadTopic() {
    return ProtocolTrafficTopics.READ;
  }

  public static String endpointMessageListTopic() {
    return ProtocolTrafficTopics.LIST;
  }
}
