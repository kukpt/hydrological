package io.github.kukpt.hydrological.monitoring;

import io.github.kukpt.hydrological.common.ProtocolTrafficTopics;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServer;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.authentication.AuthenticationProvider;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.AuthenticationHandler;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.FormLoginHandler;
import io.vertx.ext.web.handler.RedirectAuthHandler;
import io.vertx.ext.web.handler.SessionHandler;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.sstore.LocalSessionStore;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

/** One authenticated monitoring HTTP server shared by SL651 and SL427. */
public final class MonitoringWebVerticle extends AbstractVerticle {
  private static final Logger log = LoggerFactory.getLogger(MonitoringWebVerticle.class);
  private static final Map<io.vertx.core.Vertx, SharedState> STATES =
    Collections.synchronizedMap(new WeakHashMap<>());

  private final MonitoringWebOptions options;
  private final Map<String, ProtocolMonitoringProvider> providers;
  private final SharedState sharedState;
  private HttpServer httpServer;

  private MonitoringWebVerticle(
      MonitoringWebOptions options, SharedState sharedState) {
    this.options = options;
    this.sharedState = sharedState;
    this.providers = sharedState.providers;
  }

  public static void registerProvider(
      io.vertx.core.Vertx vertx, ProtocolMonitoringProvider provider) {
    if (provider == null || provider.protocol() == null) {
      throw new IllegalArgumentException("provider and provider.protocol() are required");
    }
    state(vertx).providers.put(provider.protocol().toUpperCase(), provider);
  }

  public static void unregisterProvider(
      io.vertx.core.Vertx vertx, ProtocolMonitoringProvider provider) {
    if (provider == null) return;
    SharedState state;
    synchronized (STATES) {
      state = STATES.get(vertx);
    }
    if (state == null) return;
    state.providers.remove(provider.protocol().toUpperCase(), provider);
    synchronized (STATES) {
      if (state.providers.isEmpty() && state.deployment == null) STATES.remove(vertx);
    }
  }

  /** Deploys at most one monitoring Web per Vertx instance. */
  public static Future<String> deployShared(
      io.vertx.core.Vertx vertx, MonitoringWebOptions options) {
    options.validate();
    SharedState state = state(vertx);
    synchronized (state) {
      if (state.deployment != null) {
        if (!state.options.sameServer(options)) {
          return Future.failedFuture(new IllegalStateException(
            "Monitoring Web is already configured with different host, port, or credentials"));
        }
        return state.deployment;
      }
      state.options = copy(options);
      state.deployment = vertx.deployVerticle(
        new MonitoringWebVerticle(state.options, state));
      state.deployment.onFailure(error -> {
        synchronized (state) {
          state.deployment = null;
          state.options = null;
        }
      });
      return state.deployment;
    }
  }

  @Override
  public void start(Promise<Void> startPromise) {
    AuthenticationProvider authProvider = (credentials, result) -> {
      String username = credentials.getString("username");
      String password = credentials.getString("password");
      if (options.getUserName().equals(username) && options.getPassword().equals(password)) {
        result.handle(Future.succeededFuture(
          User.create(new JsonObject().put("username", username))));
      } else {
        result.handle(Future.failedFuture("Invalid credentials"));
      }
    };

    Router router = Router.router(vertx);
    router.route().handler(SessionHandler.create(LocalSessionStore.create(vertx)));
    AuthenticationHandler auth = RedirectAuthHandler.create(authProvider, "/static/login.html");
    router.route("/static/*").handler(StaticHandler.create("webroot"));
    router.post("/do-login")
      .handler(BodyHandler.create().setHandleFileUploads(false).setBodyLimit(64 * 1024))
      .handler(FormLoginHandler.create(authProvider)
        .setDirectLoggedInOKURL("/private/dashboard.html"));
    router.route("/private/*").handler(auth);
    router.post("/private/*")
      .handler(BodyHandler.create().setHandleFileUploads(false).setBodyLimit(64 * 1024));
    router.route("/private/*").handler(StaticHandler.create("webroot/private"));

    router.get("/private/messages").handler(this::messages);
    router.get("/private/message-endpoints").handler(ctx ->
      vertx.eventBus().<JsonArray>request(ProtocolTrafficTopics.LIST, new JsonObject())
        .onSuccess(reply -> ctx.json(reply.body()))
        .onFailure(error -> serverError(ctx, "List endpoint message folders failed", error)));
    router.get("/private/online-endpoints").handler(ctx -> ctx.json(onlineEndpoints()));

    // Backward-compatible SL651 action URLs. The body may also explicitly select a protocol.
    router.post("/private/downstream/raw")
      .handler(ctx -> execute(ctx, "SL651", "downstream/raw"));
    router.post("/private/downstream/pump-control")
      .handler(ctx -> execute(ctx, "SL651", "downstream/pump-control"));
    router.post("/private/debug")
      .handler(ctx -> execute(ctx, protocol(ctx), "debug/start"));
    router.post("/private/debug/stop")
      .handler(ctx -> execute(ctx, protocol(ctx), "debug/stop"));

    router.get("/logout").handler(ctx -> {
      ctx.clearUser();
      ctx.redirect("/static/login.html");
    });
    router.get("/hp").handler(ctx -> ctx.redirect("/static/login.html"));
    router.get("/").handler(ctx -> ctx.redirect("/static/login.html"));

    vertx.createHttpServer().requestHandler(router)
      .listen(options.getPort(), options.getHost())
      .onSuccess(server -> {
        httpServer = server;
        MonitoringWebProxyTarget.publish(
          vertx, options.getHost(), server.actualPort());
        log.info("Monitoring Web Server started on " + options.getHost() + ":" + server.actualPort());
        startPromise.complete();
      })
      .onFailure(startPromise::fail);
  }

  @Override
  public void stop(Promise<Void> stopPromise) {
    if (httpServer == null) {
      stopPromise.complete();
      return;
    }
    int port = httpServer.actualPort();
    httpServer.close().onComplete(done -> {
      MonitoringWebProxyTarget.clear(vertx, port);
      synchronized (sharedState) {
        sharedState.deployment = null;
        sharedState.options = null;
      }
      synchronized (STATES) {
        STATES.remove(vertx);
      }
      if (done.succeeded()) stopPromise.complete(); else stopPromise.fail(done.cause());
    });
  }

  private void messages(RoutingContext ctx) {
    String endpointId = ctx.request().getParam("endpointId");
    if (isBlank(endpointId)) {
      badRequest(ctx, "endpointId is required");
      return;
    }
    JsonObject request = new JsonObject().put("endpointId", endpointId);
    try {
      String date = ctx.request().getParam("date");
      if (!isBlank(date)) request.put("date", LocalDate.parse(date.trim()).toString());
      putIntegerParam(ctx, request, "page");
      putIntegerParam(ctx, request, "pageSize");
    } catch (DateTimeParseException error) {
      badRequest(ctx, "date must use yyyy-MM-dd format");
      return;
    } catch (NumberFormatException error) {
      badRequest(ctx, "page and pageSize must be integers");
      return;
    }
    int page = request.getInteger("page", 1);
    int pageSize = request.getInteger("pageSize", 100);
    if (page <= 0 || pageSize <= 0) {
      badRequest(ctx, "page and pageSize must be greater than 0");
      return;
    }
    if (pageSize > 100) request.put("pageSize", 100);
    vertx.eventBus().<JsonObject>request(ProtocolTrafficTopics.READ, request)
      .onSuccess(reply -> ctx.json(reply.body()))
      .onFailure(error -> serverError(ctx, "Read endpoint messages failed", error));
  }

  private JsonArray onlineEndpoints() {
    JsonArray result = new JsonArray();
    List<String> protocols = new ArrayList<>(providers.keySet());
    Collections.sort(protocols);
    for (String protocol : protocols) {
      JsonArray endpoints = providers.get(protocol).onlineEndpoints();
      for (Object value : endpoints) {
        if (!(value instanceof JsonObject)) continue;
        JsonObject endpoint = ((JsonObject) value).copy();
        endpoint.put("protocol", protocol);
        result.add(endpoint);
      }
    }
    return result;
  }

  private void execute(RoutingContext ctx, String defaultProtocol, String action) {
    JsonObject body = ctx.body() == null ? null : ctx.body().asJsonObject();
    if (body == null) body = new JsonObject();
    if (!body.containsKey("endpointId")) {
      String endpointId = ctx.request().getParam("endpointId");
      if (!isBlank(endpointId)) body.put("endpointId", endpointId);
    }
    String selected = body.getString("protocol", defaultProtocol);
    if (isBlank(selected)) selected = protocolFromEndpoint(body.getString("endpointId"));
    ProtocolMonitoringProvider provider = selected == null
      ? null : providers.get(selected.toUpperCase());
    if (provider == null) {
      badRequest(ctx, "No monitoring provider is registered for protocol " + selected);
      return;
    }
    provider.execute(action, body)
      .onSuccess(ctx::json)
      .onFailure(error -> actionFailure(ctx, error));
  }

  private static String protocol(RoutingContext ctx) {
    String explicit = ctx.request().getParam("protocol");
    if (!isBlank(explicit)) return explicit;
    return protocolFromEndpoint(ctx.request().getParam("endpointId"));
  }

  private static String protocolFromEndpoint(String endpointId) {
    if (isBlank(endpointId)) return null;
    int separator = endpointId.indexOf(':');
    return separator > 0 ? endpointId.substring(0, separator) : "SL651";
  }

  private static void putIntegerParam(
      RoutingContext ctx, JsonObject request, String name) {
    String value = ctx.request().getParam(name);
    if (!isBlank(value)) request.put(name, Integer.parseInt(value));
  }

  private static void actionFailure(RoutingContext ctx, Throwable error) {
    Throwable cause = rootCause(error);
    int status = cause instanceof IllegalArgumentException ? 400
      : cause instanceof UnsupportedOperationException ? 405
      : cause instanceof java.util.NoSuchElementException ? 404
      : cause.getClass().getSimpleName().toLowerCase().contains("timeout") ? 504
      : cause instanceof IllegalStateException ? 409 : 500;
    jsonError(ctx, status,
      cause.getMessage() == null ? cause.getClass().getSimpleName() : cause.getMessage());
  }

  private static void serverError(RoutingContext ctx, String message, Throwable error) {
    log.error(message, error);
    jsonError(ctx, 500, error.getMessage() == null ? message : error.getMessage());
  }

  private static Throwable rootCause(Throwable error) {
    Throwable cause = error;
    while (cause.getCause() != null && cause != cause.getCause()) cause = cause.getCause();
    return cause;
  }

  private static void badRequest(RoutingContext ctx, String message) {
    jsonError(ctx, 400, message);
  }

  private static void jsonError(RoutingContext ctx, int status, String message) {
    ctx.response().setStatusCode(status)
      .putHeader("content-type", "application/json; charset=utf-8")
      .end(new JsonObject().put("success", false).put("error", message).encode());
  }

  private static boolean isBlank(String value) {
    return value == null || value.trim().isEmpty();
  }

  private static MonitoringWebOptions copy(MonitoringWebOptions source) {
    return new MonitoringWebOptions().setHost(source.getHost()).setPort(source.getPort())
      .setUserName(source.getUserName()).setPassword(source.getPassword());
  }

  private static SharedState state(io.vertx.core.Vertx vertx) {
    synchronized (STATES) {
      return STATES.computeIfAbsent(vertx, ignored -> new SharedState());
    }
  }

  private static final class SharedState {
    private final Map<String, ProtocolMonitoringProvider> providers = new ConcurrentHashMap<>();
    private MonitoringWebOptions options;
    private Future<String> deployment;
  }
}
