package io.github.kukpt.hydrological.common;

import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;

/** Consistent protocol/stage-prefixed codec logging. */
public final class ProtocolDecodeLogger {
  private static final int DEFAULT_HEX_LIMIT = 4096;

  private final Logger logger;
  private final ProtocolType protocol;

  private ProtocolDecodeLogger(Class<?> owner, ProtocolType protocol) {
    this.logger = LoggerFactory.getLogger(owner);
    this.protocol = protocol;
  }

  public static ProtocolDecodeLogger getLogger(Class<?> owner, ProtocolType protocol) {
    return new ProtocolDecodeLogger(owner, protocol);
  }

  public boolean isDebugEnabled() { return logger.isDebugEnabled(); }

  public void debug(String stage, String message) {
    if (logger.isDebugEnabled()) logger.debug(prefix(stage) + message);
  }

  public void debugRaw(String stage, String message, byte[] raw) {
    if (logger.isDebugEnabled()) logger.debug(prefix(stage) + message + ", raw=" + hex(raw));
  }

  public void warn(String stage, String message) { logger.warn(prefix(stage) + message); }

  public void error(String stage, String message, Throwable cause) {
    logger.error(prefix(stage) + message, cause);
  }

  private String prefix(String stage) { return '[' + protocol.name() + "][decode:" + stage + "] "; }

  public static String hex(byte[] bytes) {
    return hex(bytes, DEFAULT_HEX_LIMIT);
  }

  /** Encodes the complete payload for durable traffic recording. */
  public static String fullHex(byte[] bytes) {
    return hex(bytes, Integer.MAX_VALUE);
  }

  private static String hex(byte[] bytes, int limit) {
    if (bytes == null) return "null";
    int length = Math.min(bytes.length, limit);
    char[] chars = new char[length * 2];
    final char[] digits = "0123456789abcdef".toCharArray();
    for (int i = 0; i < length; i++) {
      int value = bytes[i] & 0xFF;
      chars[i * 2] = digits[value >>> 4];
      chars[i * 2 + 1] = digits[value & 0x0F];
    }
    String value = new String(chars);
    return bytes.length > length ? value + "...(totalBytes=" + bytes.length + ')' : value;
  }
}
