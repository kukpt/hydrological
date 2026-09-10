package io.github.kukpt.hydrological.monitoring;

import java.util.Objects;

/** Options for the single monitoring HTTP server shared by all protocols. */
public final class MonitoringWebOptions {
  public static final int DEFAULT_PORT = 0;
  public static final String DEFAULT_HOST = "127.0.0.1";

  private String host = DEFAULT_HOST;
  private int port = DEFAULT_PORT;
  private String userName;
  private String password;

  public String getHost() {
    return host;
  }

  public MonitoringWebOptions setHost(String host) {
    this.host = Objects.requireNonNull(host, "host");
    return this;
  }

  public int getPort() {
    return port;
  }

  public MonitoringWebOptions setPort(int port) {
    if (port < 0 || port > 65535) {
      throw new IllegalArgumentException("port must be in 0..65535");
    }
    this.port = port;
    return this;
  }

  public String getUserName() {
    return userName;
  }

  public MonitoringWebOptions setUserName(String userName) {
    this.userName = userName;
    return this;
  }

  public String getPassword() {
    return password;
  }

  public MonitoringWebOptions setPassword(String password) {
    this.password = password;
    return this;
  }

  public void validate() {
    if (isBlank(userName)) throw new IllegalArgumentException("Monitoring Web username is required");
    if (isBlank(password)) throw new IllegalArgumentException("Monitoring Web password is required");
    if (password.length() < 12) {
      throw new IllegalArgumentException("Monitoring Web password must be at least 12 characters");
    }
  }

  boolean sameServer(MonitoringWebOptions other) {
    return other != null && host.equals(other.host) && port == other.port
      && Objects.equals(userName, other.userName) && Objects.equals(password, other.password);
  }

  private static boolean isBlank(String value) {
    return value == null || value.trim().isEmpty();
  }
}
