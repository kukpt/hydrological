package io.github.kukpt.sl651;

import io.github.kukpt.sl651.utils.FrameEndType;
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
  private final static int TIMEOUT_ON_CONNECT = 180;
  private final static FrameEndType FRAME_END_TYPE = FrameEndType.ESC_MODE;
  private final static boolean ENABLE_METRICS_WEB = false;
  private final static String METRICS_WEB_USERNAME = "wisetion";
  private final static String METRICS_WEB_PASSWORD = "wisetion";

  private boolean isM2LinkMode;

  private short centralStationAddress;

  private int protocolPassword;

  private int timeoutOnConnect;

  private FrameEndType frameEndType;

  private boolean enableMetricsWeb;

  private String metricsWebUserName;

  private String metricsWebPassword;

  public void init() {
    this.setPort(DEFAULT_PORT);
    this.setM2LinkMode(M2_LINK_MODE);
    this.setCentralStationAddress(CENTRAL_STATION_ADDRESS);
    this.setProtocolPassword(PROTOCOL_PASSWORD);
    this.timeoutOnConnect = TIMEOUT_ON_CONNECT;
    this.frameEndType = FRAME_END_TYPE;
    this.enableMetricsWeb = ENABLE_METRICS_WEB;
    this.metricsWebUserName = METRICS_WEB_USERNAME;
    this.metricsWebPassword = METRICS_WEB_PASSWORD;
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

  public HydrologicalServerOptions setFrameEndType(FrameEndType ft) {
    this.frameEndType = ft;
    return this;
  }

  public FrameEndType getFrameEndType() {
    return this.frameEndType;
  }

  public HydrologicalServerOptions enableMetricsWeb(boolean enableMetricsWeb) {
    this.enableMetricsWeb = enableMetricsWeb;
    return this;
  }


  public HydrologicalServerOptions setMetricsWebUserName(String metricsWebUserName) {
    this.metricsWebUserName = metricsWebUserName;
    return this;
  }

  public HydrologicalServerOptions setMetricsWebPassword(String metricsWebPassword) {
    this.metricsWebPassword = metricsWebPassword;
    return this;
  }


  public boolean isEnableMetricsWeb() {
    return enableMetricsWeb;
  }

  public String getMetricsWebUserName() {
    return metricsWebUserName;
  }

  public String getMetricsWebPassword() {
    return metricsWebPassword;
  }


}
