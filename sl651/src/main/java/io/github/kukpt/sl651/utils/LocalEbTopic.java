package io.github.kukpt.sl651.utils;

public final class LocalEbTopic {
  private LocalEbTopic() {}
  private static String ENABLE_ENDPOINT_DEBUG = "hydrological.endpoint.debug.enable.%s";

  private static String DISABLE_ENDPOINT_DEBUG = "hydrological.endpoint.debug.disable.%s";

  private static final String ENDPOINT_MESSAGE_APPEND = "hydrological.endpoint.message.append";

  private static final String ENDPOINT_MESSAGE_READ = "hydrological.endpoint.message.read";

  private static final String ENDPOINT_MESSAGE_LIST = "hydrological.endpoint.message.list";

  public static String generateEnableEndpointDebugTopic(String endpointId) {
    return String.format(ENABLE_ENDPOINT_DEBUG, endpointId);
  }

  public static String generateDisableEndpointDebugTopic(String endpointId) {
    return String.format(DISABLE_ENDPOINT_DEBUG, endpointId);
  }


  public static String endpointMessageAppendTopic() {
    return ENDPOINT_MESSAGE_APPEND;
  }

  public static String endpointMessageReadTopic() {
    return ENDPOINT_MESSAGE_READ;
  }

  public static String endpointMessageListTopic() {
    return ENDPOINT_MESSAGE_LIST;
  }
}
