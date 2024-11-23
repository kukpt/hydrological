package io.github.kukpt.sl651;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.NetServerOptions;

@DataObject
@JsonGen(publicConverter = false)
public class HydrologicalServerOptions extends NetServerOptions {

  private final static int DEFAULT_PORT = 11883;
  private final static boolean M2_LINK_MODE = true;
  private final static short CENTRAL_STATION_ADDRESS = 0x01;
  private final static int PROTOCOL_PASSWORD = 0x1234;
  private final static int TIMEOUT_ON_CONNECT = 60;


  private boolean isM2LinkMode;

  private short centralStationAddress;

  private int protocolPassword;

  private int timeoutOnConnect;

  public void init() {
    this.setPort(DEFAULT_PORT);
    this.setM2LinkMode(M2_LINK_MODE);
    this.setCentralStationAddress(CENTRAL_STATION_ADDRESS);
    this.setProtocolPassword(PROTOCOL_PASSWORD);
    this.timeoutOnConnect = TIMEOUT_ON_CONNECT;
  }

  public HydrologicalServerOptions() {
    super();
    init();
  }

  public HydrologicalServerOptions(JsonObject json) {
    super(json);
    init();
    HydrologicalServerOptionsConverter.fromJson(json, this);
  }

  public HydrologicalServerOptions setM2LinkMode(boolean use) {
    this.isM2LinkMode = use;
    return this;
  }

  public boolean isM2LinkMode() {
    return this.isM2LinkMode;
  }

  public HydrologicalServerOptions setProtocolPassword(int password) {
    this.protocolPassword = password;
    return this;
  }

  public int getProtocolPassword() {
    return this.protocolPassword;
  }

  public HydrologicalServerOptions setCentralStationAddress(short address) {
    this.centralStationAddress = address;
    return this;
  }

  public short getCentralStationAddress() {
    return this.centralStationAddress;
  }


  public HydrologicalServerOptions setTimeoutOnConnect(int timeoutOnConnect) {
    this.timeoutOnConnect = timeoutOnConnect;
    return this;
  }
  public int getTimeoutOnConnect() {
    return this.timeoutOnConnect;
  }

  public HydrologicalServerOptions setPort(int port) {
    super.setPort(port);
    return this;
  }

  public HydrologicalServerOptions setHost(String host) {
    super.setHost(host);
    return this;
  }
}
