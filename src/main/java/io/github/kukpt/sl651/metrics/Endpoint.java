package io.github.kukpt.sl651.metrics;

import java.time.LocalDateTime;

public class Endpoint {

  public Endpoint(String endpointId, String remoteAddr, String password) {
    this.endpointId = endpointId;
    this.remoteAddr = remoteAddr;
    this.password = password;
    this.connectionTime = LocalDateTime.now();
  }

  private String endpointId;

  private String remoteAddr;

  private LocalDateTime connectionTime;

  private String password;

  public String getEndpointId() {
    return endpointId;
  }

  public String getRemoteAddr() {
    return remoteAddr;
  }

  public LocalDateTime getConnectionTime() {
    return connectionTime;
  }

  public String getPassword() {
    return password;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("Endpoint{");
    sb.append("endpointId='").append(endpointId).append('\'');
    sb.append(", remoteAddr='").append(remoteAddr).append('\'');
    sb.append(", connectionTime=").append(connectionTime);
    sb.append(", password='").append(password).append('\'');
    sb.append('}');
    return sb.toString();
  }
}
