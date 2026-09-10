package io.github.kukpt.sl427;

import io.github.kukpt.hydrological.common.ProtocolTrafficRecorder;
import io.github.kukpt.sl427.model.ProtocolMessage;
import io.github.kukpt.sl427.model.TerminalWorkMode;
import io.github.kukpt.sl427.result.ParseResult;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;

import java.util.Locale;
import java.util.Objects;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;

/** Standalone SL/T 427-2021 TCP server entry point. */
public class Sl427MainVerticle extends AbstractVerticle {

  public static final int DEFAULT_PORT = Sl427ServerOptions.DEFAULT_PORT;
  public static final String LOG_LEVEL_PROPERTY = "sl427.log.level";
  public static final String LOG_LEVEL_ENV = "SL427_LOG_LEVEL";

  private static final Logger log = LoggerFactory.getLogger(Sl427MainVerticle.class);

  private final Sl427ServerOptions options;
  private Sl427Server server;

  public Sl427MainVerticle() {
    this(DEFAULT_PORT, TerminalWorkMode.COMPATIBLE, ProtocolTrafficRecorder.NOOP);
  }

  /** Allows tests and embedding applications to select another port; zero chooses an ephemeral port. */
  public Sl427MainVerticle(int port) {
    this(port, TerminalWorkMode.COMPATIBLE, ProtocolTrafficRecorder.NOOP);
  }

  public Sl427MainVerticle(int port, TerminalWorkMode confirmationWorkMode,
      ProtocolTrafficRecorder trafficRecorder) {
    this(new Sl427ServerOptions()
      .setPort(port)
      .setConfirmationWorkMode(confirmationWorkMode)
      .setTrafficRecorder(trafficRecorder));
  }

  public Sl427MainVerticle(Sl427ServerOptions options) {
    this.options = Objects.requireNonNull(options, "options");
  }

  public static void main(String[] args) {
    Vertx vertx = Vertx.vertx();
    vertx.deployVerticle(new Sl427MainVerticle())
      .onFailure(error -> {
        log.error("SL427 server deployment failed", error);
        vertx.close();
      });
  }

  @Override
  public void start(Promise<Void> startPromise) {
    configureLogLevel();
    server = Sl427Server.create(vertx, options)
      .endpointHandler(endpoint -> {
        log.info("SL427 endpoint connected, remote=" + endpoint.remoteAddress());
        endpoint.messageHandler(message -> logMessage(endpoint, message));
        endpoint.decodeFailureHandler(result -> logDecodeFailure(endpoint, result));
        endpoint.exceptionHandler(error ->
          log.error("SL427 endpoint error, remote=" + endpoint.remoteAddress(), error));
        endpoint.closeHandler(closed ->
          log.info("SL427 endpoint disconnected, remote=" + closed.endpointId()));
      })
      .onlineHandler(endpoint -> log.info("SL427 station online, endpointId="
        + endpoint.endpointId() + ", remote=" + endpoint.remoteAddress()))
      .offlineHandler(endpoint -> log.info("SL427 station offline, endpointId="
        + endpoint.endpointId() + ", remote=" + endpoint.remoteAddress()))
      .exceptionHandler(error -> log.error("SL427 server error", error));

    server.listen()
      .onSuccess(listening -> {
        log.info("SL427 server started, port=" + listening.actualPort());
        startPromise.complete();
      })
      .onFailure(error -> {
        log.error("SL427 server failed to listen on port " + options.getPort(), error);
        startPromise.fail(error);
      });
  }

  @Override
  public void stop(Promise<Void> stopPromise) {
    if (server == null) {
      stopPromise.complete();
      return;
    }
    server.close().onComplete(stopPromise);
  }

  public int actualPort() {
    return server == null ? -1 : server.actualPort();
  }

  private static void logMessage(Sl427Endpoint endpoint, ProtocolMessage message) {
    log.info("SL427 message decoded, remote=" + endpoint.endpointId()
      + ", address=" + message.frame().address()
      + ", afn=" + message.frame().afn()
      + ", messageType=" + message.getClass().getSimpleName());
  }

  private static void logDecodeFailure(Sl427Endpoint endpoint,
      ParseResult<ProtocolMessage> result) {
    log.warn("SL427 message decode result, remote=" + endpoint.endpointId()
      + ", status=" + result.status() + ", issues=" + result.issues());
  }

  private static void configureLogLevel() {
    String configured = System.getProperty(LOG_LEVEL_PROPERTY);
    if (isBlank(configured)) configured = System.getenv(LOG_LEVEL_ENV);
    if (isBlank(configured)) return;

    Level level;
    try {
      level = toJulLevel(configured);
    } catch (IllegalArgumentException error) {
      log.warn("Ignoring invalid SL427 log level '" + configured
        + "'; supported values: TRACE, DEBUG, INFO, WARN, ERROR, OFF");
      return;
    }

    java.util.logging.Logger.getLogger("io.github.kukpt.sl427").setLevel(level);
    java.util.logging.Logger root = LogManager.getLogManager().getLogger("");
    if (root != null) {
      for (Handler handler : root.getHandlers()) {
        if (handler.getLevel().intValue() > level.intValue()) handler.setLevel(level);
      }
    }
    log.info("SL427 log level set to " + configured.trim().toUpperCase(Locale.ROOT));
  }

  private static Level toJulLevel(String value) {
    String normalized = value.trim().toUpperCase(Locale.ROOT);
    if ("TRACE".equals(normalized)) return Level.FINER;
    if ("DEBUG".equals(normalized)) return Level.FINE;
    if ("INFO".equals(normalized)) return Level.INFO;
    if ("WARN".equals(normalized) || "WARNING".equals(normalized)) return Level.WARNING;
    if ("ERROR".equals(normalized)) return Level.SEVERE;
    if ("OFF".equals(normalized)) return Level.OFF;
    throw new IllegalArgumentException("unsupported log level");
  }

  private static boolean isBlank(String value) {
    return value == null || value.trim().isEmpty();
  }
}
