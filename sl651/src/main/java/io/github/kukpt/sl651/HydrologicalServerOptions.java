package io.github.kukpt.sl651;

import io.github.kukpt.sl651.utils.FrameEndType;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.NetServerOptions;

import java.nio.file.Paths;

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
  private final static boolean ENABLE_HTTP_PROXY = false;
  public final static String METRICS_WEB_USERNAME_ENV = "HY_METRICS_USERNAME";
  public final static String METRICS_WEB_PASSWORD_ENV = "HY_METRICS_PASSWORD";
  private final static int MIN_METRICS_WEB_PASSWORD_LENGTH = 12;
  private static final String METRICS_LOG_BASE_DIR =
      Paths.get(System.getProperty("user.dir"), "hy-data").toString();

  private boolean isM2LinkMode;

  private short centralStationAddress;

  private int protocolPassword;

  private int timeoutOnConnect;

  private FrameEndType frameEndType;

  private boolean enableMetricsWeb;

  private boolean enableHttpProxy;

  private String metricsWebUserName;

  private String metricsWebPassword;

  private String metricsLogBaseDir;

  public void init() {
    this.setPort(DEFAULT_PORT);
    this.setM2LinkMode(M2_LINK_MODE);
    this.setCentralStationAddress(CENTRAL_STATION_ADDRESS);
    this.setProtocolPassword(PROTOCOL_PASSWORD);
    this.timeoutOnConnect = TIMEOUT_ON_CONNECT;
    this.frameEndType = FRAME_END_TYPE;
    this.enableMetricsWeb = ENABLE_METRICS_WEB;
    this.enableHttpProxy = ENABLE_HTTP_PROXY;
    this.metricsWebUserName = envOrNull(METRICS_WEB_USERNAME_ENV);
    this.metricsWebPassword = envOrNull(METRICS_WEB_PASSWORD_ENV);
    this.metricsLogBaseDir = METRICS_LOG_BASE_DIR;
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

  /**
   * Enables HTTP protocol detection and proxying on the SL651 TCP port.
   * Disabled by default because exposing two protocols on one port increases
   * the attack surface. Metrics Web must also be enabled for this option to
   * take effect.
   */
  public HydrologicalServerOptions enableHttpProxy(boolean enableHttpProxy) {
    return setEnableHttpProxy(enableHttpProxy);
  }

  public HydrologicalServerOptions setEnableHttpProxy(boolean enableHttpProxy) {
    this.enableHttpProxy = enableHttpProxy;
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

  public String getMetricsLogBaseDir() {
    return metricsLogBaseDir;
  }

  public HydrologicalServerOptions setMetricsLogBaseDir(String metricsLogBaseDir) {
    this.metricsLogBaseDir = metricsLogBaseDir;
    return this;
  }

  public boolean isEnableMetricsWeb() {
    return enableMetricsWeb;
  }

  public boolean isEnableHttpProxy() {
    return enableHttpProxy;
  }

  public String getMetricsWebUserName() {
    return metricsWebUserName;
  }

  public String getMetricsWebPassword() {
    return metricsWebPassword;
  }

  public void validateMetricsWebCredentials() {
    if (!this.enableMetricsWeb) {
      return;
    }
    if (isBlank(this.metricsWebUserName)) {
      throw new IllegalArgumentException("Metrics Web username is required when metrics web is enabled. "
          + "Set it with setMetricsWebUserName(...) or environment variable " + METRICS_WEB_USERNAME_ENV + ".");
    }
    if (isBlank(this.metricsWebPassword)) {
      throw new IllegalArgumentException("Metrics Web password is required when metrics web is enabled. "
          + "Set it with setMetricsWebPassword(...) or environment variable " + METRICS_WEB_PASSWORD_ENV + ".");
    }
    if (this.metricsWebPassword.length() < MIN_METRICS_WEB_PASSWORD_LENGTH) {
      throw new IllegalArgumentException("Metrics Web password must be at least "
          + MIN_METRICS_WEB_PASSWORD_LENGTH + " characters.");
    }
  }

  private static String envOrNull(String name) {
    String value = System.getenv(name);
    return isBlank(value) ? null : value;
  }

  private static boolean isBlank(String value) {
    return value == null || value.trim().isEmpty();
  }

}
