package io.github.kukpt.sl427;

import io.github.kukpt.hydrological.common.ProtocolTrafficRecorder;
import io.github.kukpt.sl427.context.EmptyProtocolContext;
import io.github.kukpt.sl427.context.ProtocolContext;
import io.github.kukpt.sl427.context.ProtocolContextResolver;
import io.github.kukpt.sl427.frame.StationAddress;
import io.github.kukpt.sl427.frame.AddressMode;
import io.github.kukpt.sl427.model.TerminalWorkMode;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.NetServerOptions;

import java.nio.file.Paths;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/** TCP and protocol options for {@link Sl427Server}. */
public class Sl427ServerOptions extends NetServerOptions {

  public static final int DEFAULT_PORT = 11824;
  public static final String DEFAULT_HOST = "0.0.0.0";
  public static final int DEFAULT_TIMEOUT_ON_CONNECT = 180;
  public static final String METRICS_WEB_USERNAME_ENV = "HY_METRICS_USERNAME";
  public static final String METRICS_WEB_PASSWORD_ENV = "HY_METRICS_PASSWORD";
  public static final String DEFAULT_TRAFFIC_LOG_BASE_DIR =
    Paths.get(System.getProperty("user.dir"), "hy-data").toString();

  private TerminalWorkMode confirmationWorkMode;
  private AddressMode addressMode;
  private boolean autoConfirmation;
  private int timeoutOnConnect;
  private boolean enableTrafficStore;
  private boolean enableMetricsWeb;
  private boolean enableHttpProxy;
  private boolean includeParsedTrafficData;
  private String metricsWebUserName;
  private String metricsWebPassword;
  private String trafficLogBaseDir;
  private ProtocolTrafficRecorder trafficRecorder;
  private ProtocolContext protocolContext;
  private ProtocolContextResolver protocolContextResolver;

  public Sl427ServerOptions() {
    super();
    setPort(DEFAULT_PORT);
    setHost(DEFAULT_HOST);
    initProtocolDefaults();
    setTimeoutOnConnect(DEFAULT_TIMEOUT_ON_CONNECT);
  }

  public Sl427ServerOptions(JsonObject json) {
    super(json);
    if (json == null || !json.containsKey("port")) setPort(DEFAULT_PORT);
    if (json == null || !json.containsKey("host")) setHost(DEFAULT_HOST);
    initProtocolDefaults();
    if (json != null) {
      String workMode = json.getString("confirmationWorkMode");
      if (workMode != null) setConfirmationWorkMode(TerminalWorkMode.valueOf(workMode));
      String configuredAddressMode = json.getString("addressMode");
      if (configuredAddressMode != null) setAddressMode(AddressMode.valueOf(configuredAddressMode));
      Boolean automatic = json.getBoolean("autoConfirmation");
      if (automatic != null) setAutoConfirmation(automatic);
      Integer timeout = json.getInteger("timeoutOnConnect");
      if (timeout != null) {
        setTimeoutOnConnect(timeout);
      } else if (json.containsKey("idleTimeout")) {
        timeoutOnConnect = getIdleTimeout();
      } else {
        setTimeoutOnConnect(DEFAULT_TIMEOUT_ON_CONNECT);
      }
      Boolean storeEnabled = json.getBoolean("enableTrafficStore");
      if (storeEnabled != null) setEnableTrafficStore(storeEnabled);
      String storeDir = json.getString("trafficLogBaseDir");
      if (storeDir != null) setTrafficLogBaseDir(storeDir);
      Boolean webEnabled = json.getBoolean("enableMetricsWeb");
      if (webEnabled != null) enableMetricsWeb(webEnabled);
      Boolean proxyEnabled = json.getBoolean("enableHttpProxy");
      if (proxyEnabled != null) enableHttpProxy(proxyEnabled);
      Boolean parsedTraffic = json.getBoolean("includeParsedTrafficData");
      if (parsedTraffic != null) setIncludeParsedTrafficData(parsedTraffic);
      String webUser = json.getString("metricsWebUserName");
      if (webUser != null) setMetricsWebUserName(webUser);
      String webPassword = json.getString("metricsWebPassword");
      if (webPassword != null) setMetricsWebPassword(webPassword);
    } else {
      setTimeoutOnConnect(DEFAULT_TIMEOUT_ON_CONNECT);
    }
  }

  private void initProtocolDefaults() {
    confirmationWorkMode = TerminalWorkMode.COMPATIBLE;
    addressMode = AddressMode.ADMINISTRATIVE;
    autoConfirmation = true;
    timeoutOnConnect = DEFAULT_TIMEOUT_ON_CONNECT;
    enableTrafficStore = true;
    enableMetricsWeb = false;
    enableHttpProxy = false;
    includeParsedTrafficData = true;
    metricsWebUserName = envOrNull(METRICS_WEB_USERNAME_ENV);
    metricsWebPassword = envOrNull(METRICS_WEB_PASSWORD_ENV);
    trafficLogBaseDir = DEFAULT_TRAFFIC_LOG_BASE_DIR;
    trafficRecorder = ProtocolTrafficRecorder.NOOP;
    protocolContext = EmptyProtocolContext.INSTANCE;
  }

  @Override
  public Sl427ServerOptions setPort(int port) {
    if (port < 0 || port > 65535) throw new IllegalArgumentException("port must be in 0..65535");
    super.setPort(port);
    return this;
  }

  @Override
  public Sl427ServerOptions setHost(String host) {
    super.setHost(host);
    return this;
  }

  public TerminalWorkMode getConfirmationWorkMode() {
    return confirmationWorkMode;
  }

  public Sl427ServerOptions setConfirmationWorkMode(TerminalWorkMode confirmationWorkMode) {
    this.confirmationWorkMode = Objects.requireNonNull(confirmationWorkMode, "confirmationWorkMode");
    return this;
  }

  public AddressMode getAddressMode() {
    return addressMode;
  }

  public Sl427ServerOptions setAddressMode(AddressMode addressMode) {
    this.addressMode = Objects.requireNonNull(addressMode, "addressMode");
    return this;
  }

  public boolean isAutoConfirmation() {
    return autoConfirmation;
  }

  public Sl427ServerOptions setAutoConfirmation(boolean autoConfirmation) {
    this.autoConfirmation = autoConfirmation;
    return this;
  }

  public int getTimeoutOnConnect() {
    return timeoutOnConnect;
  }

  public Sl427ServerOptions setTimeoutOnConnect(int timeoutOnConnect) {
    if (timeoutOnConnect < 0) {
      throw new IllegalArgumentException("timeoutOnConnect must be greater than or equal to 0");
    }
    this.timeoutOnConnect = timeoutOnConnect;
    super.setIdleTimeout(timeoutOnConnect);
    super.setIdleTimeoutUnit(TimeUnit.SECONDS);
    return this;
  }

  public boolean isEnableTrafficStore() {
    return enableTrafficStore;
  }

  public Sl427ServerOptions enableTrafficStore(boolean enableTrafficStore) {
    return setEnableTrafficStore(enableTrafficStore);
  }

  public Sl427ServerOptions setEnableTrafficStore(boolean enableTrafficStore) {
    this.enableTrafficStore = enableTrafficStore;
    return this;
  }

  public String getTrafficLogBaseDir() {
    return trafficLogBaseDir;
  }

  public Sl427ServerOptions setTrafficLogBaseDir(String trafficLogBaseDir) {
    if (trafficLogBaseDir == null || trafficLogBaseDir.trim().isEmpty()) {
      throw new IllegalArgumentException("trafficLogBaseDir must not be blank");
    }
    this.trafficLogBaseDir = trafficLogBaseDir;
    return this;
  }

  public boolean isEnableMetricsWeb() {
    return enableMetricsWeb;
  }

  public Sl427ServerOptions enableMetricsWeb(boolean enableMetricsWeb) {
    return setEnableMetricsWeb(enableMetricsWeb);
  }

  public Sl427ServerOptions setEnableMetricsWeb(boolean enableMetricsWeb) {
    this.enableMetricsWeb = enableMetricsWeb;
    return this;
  }

  public boolean isEnableHttpProxy() {
    return enableHttpProxy;
  }

  /** Enables HTTP detection on the SL427 TCP port and proxies it to the shared monitoring Web. */
  public Sl427ServerOptions enableHttpProxy(boolean enableHttpProxy) {
    return setEnableHttpProxy(enableHttpProxy);
  }

  public Sl427ServerOptions setEnableHttpProxy(boolean enableHttpProxy) {
    this.enableHttpProxy = enableHttpProxy;
    return this;
  }

  public boolean isIncludeParsedTrafficData() {
    return includeParsedTrafficData;
  }

  public Sl427ServerOptions setIncludeParsedTrafficData(boolean includeParsedTrafficData) {
    this.includeParsedTrafficData = includeParsedTrafficData;
    return this;
  }

  public String getMetricsWebUserName() {
    return metricsWebUserName;
  }

  public Sl427ServerOptions setMetricsWebUserName(String metricsWebUserName) {
    this.metricsWebUserName = metricsWebUserName;
    return this;
  }

  public String getMetricsWebPassword() {
    return metricsWebPassword;
  }

  public Sl427ServerOptions setMetricsWebPassword(String metricsWebPassword) {
    this.metricsWebPassword = metricsWebPassword;
    return this;
  }

  public void validateMetricsWebCredentials() {
    if (!enableMetricsWeb) return;
    if (isBlank(metricsWebUserName)) {
      throw new IllegalArgumentException("Metrics Web username is required when metrics web is enabled. "
        + "Set it with setMetricsWebUserName(...) or environment variable "
        + METRICS_WEB_USERNAME_ENV + ".");
    }
    if (isBlank(metricsWebPassword)) {
      throw new IllegalArgumentException("Metrics Web password is required when metrics web is enabled. "
        + "Set it with setMetricsWebPassword(...) or environment variable "
        + METRICS_WEB_PASSWORD_ENV + ".");
    }
    if (metricsWebPassword.length() < 12) {
      throw new IllegalArgumentException("Metrics Web password must be at least 12 characters.");
    }
  }

  public ProtocolTrafficRecorder getTrafficRecorder() {
    return trafficRecorder;
  }

  public Sl427ServerOptions setTrafficRecorder(ProtocolTrafficRecorder trafficRecorder) {
    this.trafficRecorder = trafficRecorder == null ? ProtocolTrafficRecorder.NOOP : trafficRecorder;
    return this;
  }

  public ProtocolContext getProtocolContext() {
    return protocolContext;
  }

  public Sl427ServerOptions setProtocolContext(ProtocolContext protocolContext) {
    this.protocolContext = protocolContext == null ? EmptyProtocolContext.INSTANCE : protocolContext;
    return this;
  }

  public ProtocolContextResolver getProtocolContextResolver() {
    return protocolContextResolver;
  }

  public Sl427ServerOptions setProtocolContextResolver(ProtocolContextResolver protocolContextResolver) {
    this.protocolContextResolver = protocolContextResolver;
    return this;
  }

  public ProtocolContext resolveProtocolContext(StationAddress address) {
    if (protocolContextResolver == null) return protocolContext;
    ProtocolContext resolved = protocolContextResolver.resolve(address);
    return resolved == null ? EmptyProtocolContext.INSTANCE : resolved;
  }

  private static String envOrNull(String name) {
    String value = System.getenv(name);
    return isBlank(value) ? null : value;
  }

  private static boolean isBlank(String value) {
    return value == null || value.trim().isEmpty();
  }
}
