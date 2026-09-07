package io.github.kukpt.sl651;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.github.kukpt.sl651.codec.PumpStationControlResponseMessage;
import io.github.kukpt.sl651.codec.RawDownstreamPayload;
import io.github.kukpt.sl651.utils.LocalEbTopic;
import io.netty.buffer.ByteBufUtil;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServer;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.jackson.DatabindCodec;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.authentication.AuthenticationProvider;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.*;
import io.vertx.ext.web.sstore.LocalSessionStore;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;


public class MetricsWebVerticle extends AbstractVerticle {

  private static final Logger log = LoggerFactory.getLogger(MetricsWebVerticle.class);

  private final String userName;

  private final String password;

  private final OnlineEndpointRegistry onlineEndpoints;


  public MetricsWebVerticle(
    String userName, String password, OnlineEndpointRegistry onlineEndpoints) {
    this.userName = userName;
    this.password = password;
    this.onlineEndpoints = onlineEndpoints;
  }

  static {
    DatabindCodec.mapper().registerModule(new JavaTimeModule());
    DatabindCodec.mapper().disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
  }

  @Override
  public void start(Promise<Void> startPromise) {

    AuthenticationProvider authProvider = (credentials, result) -> {
      String username = credentials.getString("username");
      String password = credentials.getString("password");


      if (this.userName.equals(username) && this.password.equals(password)) {
        result.handle(Future.succeededFuture(User.create(new JsonObject().put("username", username))));
      } else {
        result.handle(Future.failedFuture("Invalid credentials"));
      }
    };
    Router router = Router.router(vertx);

    router.route().handler(SessionHandler.create(LocalSessionStore.create(vertx)));
    AuthenticationHandler rh = RedirectAuthHandler.create(authProvider, "/static/login.html");


    router.route("/static/*").handler(StaticHandler.create("webroot"));

    router.post("/do-login")
        .handler(BodyHandler.create()
            .setHandleFileUploads(false)
            .setBodyLimit(64 * 1024))
        .handler(FormLoginHandler.create(authProvider)
            .setDirectLoggedInOKURL("/private/dashboard.html"));

    // --- 4. 路由保护拦截器 ---
    // 所有访问 /private/* 的请求，如果没有登录，自动重定向到 /static/login.html
    router.route("/private/*").handler(rh);

    router.post("/private/downstream/*").handler(BodyHandler.create()
      .setHandleFileUploads(false)
      .setBodyLimit(64 * 1024));

    // 映射受保护的静态资源（把受保护的 HTML 放在另一个路径下）
    router.route("/private/*").handler(StaticHandler.create("webroot/private"));

    router.get("/private/messages").handler(ctx -> {
      String endpointId = ctx.request().getParam("endpointId");
      if (endpointId == null || endpointId.trim().isEmpty()) {
        ctx.response().setStatusCode(400).end("endpointId is required");
        return;
      }
      String pageParam = ctx.request().getParam("page");
      String pageSizeParam = ctx.request().getParam("pageSize");
      String dateParam = ctx.request().getParam("date");
      JsonObject request = new JsonObject().put("endpointId", endpointId);
      if (dateParam != null && !dateParam.trim().isEmpty()) {
        try {
          request.put("date", LocalDate.parse(dateParam.trim()).toString());
        } catch (DateTimeParseException e) {
          ctx.response().setStatusCode(400).end("date must use yyyy-MM-dd format");
          return;
        }
      }
      try {
        if (pageParam != null && !pageParam.trim().isEmpty()) {
          request.put("page", Integer.parseInt(pageParam));
        }
        if (pageSizeParam != null && !pageSizeParam.trim().isEmpty()) {
          request.put("pageSize", Integer.parseInt(pageSizeParam));
        }
      } catch (NumberFormatException e) {
        ctx.response().setStatusCode(400).end("page and pageSize must be integers");
        return;
      }
      Integer page = request.getInteger("page", 1);
      Integer pageSize = request.getInteger("pageSize", 100);
      if (page <= 0 || pageSize <= 0) {
        ctx.response().setStatusCode(400).end("page and pageSize must be greater than 0");
        return;
      }
      if (pageSize > 100) {
        request.put("pageSize", 100);
      }
      vertx.eventBus().<JsonObject>request(LocalEbTopic.endpointMessageReadTopic(), request)
           .onSuccess(reply -> ctx.json(reply.body()))
           .onFailure(err -> {
             log.error("Read endpoint messages failed", err);
             ctx.response().setStatusCode(500).end(err.getMessage());
           });
    });

    router.get("/private/message-endpoints").handler(ctx ->
        vertx.eventBus().<JsonArray>request(LocalEbTopic.endpointMessageListTopic(), new JsonObject())
             .onSuccess(reply -> ctx.json(reply.body()))
             .onFailure(err -> {
               log.error("List endpoint message folders failed", err);
               ctx.response().setStatusCode(500).end(err.getMessage());
             })
    );

    router.get("/private/online-endpoints").handler(ctx -> ctx.json(onlineEndpoints.toJson()));

    router.post("/private/downstream/raw").handler(ctx -> {
      JsonObject body = ctx.body().asJsonObject();
      if (body == null) {
        badRequest(ctx, "JSON body is required");
        return;
      }
      String endpointId = requiredText(body, "endpointId");
      String payloadHex = requiredText(body, "payloadHex");
      Integer functionType = body.getInteger("functionType");
      Integer frameControlType = body.getInteger("frameControlType", 0x05);
      Long timeout = body.getLong("timeout", 30L);
      if (endpointId == null || payloadHex == null) {
        badRequest(ctx, "endpointId and payloadHex are required");
        return;
      }
      if (functionType == null || functionType < 0 || functionType > 0xFF) {
        badRequest(ctx, "functionType must be between 0 and 255");
        return;
      }
      if (frameControlType < 0 || frameControlType > 0xFF) {
        badRequest(ctx, "frameControlType must be between 0 and 255");
        return;
      }
      if (!validTimeout(timeout)) {
        badRequest(ctx, "timeout must be between 1 and 300 seconds");
        return;
      }
      String normalizedHex = payloadHex.replaceAll("\\s+", "");
      if (normalizedHex.isEmpty() || (normalizedHex.length() & 1) != 0
        || !normalizedHex.matches("[0-9a-fA-F]+")) {
        badRequest(ctx, "payloadHex must contain an even number of hexadecimal characters");
        return;
      }
      HydrologicalEndpoint endpoint = onlineEndpoints.get(endpointId);
      if (endpoint == null) {
        notFound(ctx, "endpoint is offline: " + endpointId);
        return;
      }
      endpoint.downstream(functionType, RawDownstreamPayload.fromHex(normalizedHex),
          frameControlType.shortValue(), timeout)
        .onSuccess(response -> ctx.json(new JsonObject()
          .put("success", true)
          .put("endpointId", endpointId)
          .put("functionType", response.functionType())
          .put("streamId", response.streamId())
          .put("responsePayloadHex", ByteBufUtil.hexDump(response.payload()))))
        .onFailure(error -> downstreamFailure(ctx, error));
    });

    router.post("/private/downstream/pump-control").handler(ctx -> {
      JsonObject body = ctx.body().asJsonObject();
      if (body == null) {
        badRequest(ctx, "JSON body is required");
        return;
      }
      String endpointId = requiredText(body, "endpointId");
      Integer command = body.getInteger("command");
      Long timeout = body.getLong("timeout", 30L);
      if (endpointId == null || command == null) {
        badRequest(ctx, "endpointId and command are required");
        return;
      }
      if (command < 0 || command > 0xFF) {
        badRequest(ctx, "command must be between 0 and 255");
        return;
      }
      if (!validTimeout(timeout)) {
        badRequest(ctx, "timeout must be between 1 and 300 seconds");
        return;
      }
      HydrologicalEndpoint endpoint = onlineEndpoints.get(endpointId);
      if (endpoint == null) {
        notFound(ctx, "endpoint is offline: " + endpointId);
        return;
      }
      endpoint.pumpStationControl(command.shortValue(), timeout)
        .onSuccess(response -> ctx.json(pumpResponse(endpointId, response)))
        .onFailure(error -> downstreamFailure(ctx, error));
    });
    router.post("/private/debug").handler(ctx -> {
      String endpointId = ctx.request().getParam("endpointId");
      vertx.eventBus().send(LocalEbTopic.generateEnableEndpointDebugTopic(endpointId), new JsonObject());
      ctx.end();
    });

    router.post("/private/debug/stop").handler(ctx -> {
      String endpointId = ctx.request().getParam("endpointId");
      vertx.eventBus().send(LocalEbTopic.generateDisableEndpointDebugTopic(endpointId), new JsonObject());
      ctx.end();
    });

    // 退出登录
    router.get("/logout").handler(ctx -> {
      ctx.clearUser();
      ctx.redirect("/static/login.html");
    });

    // 根目录自动跳转到登录
    router.get("/hp").handler(ctx -> ctx.redirect("/static/login.html"));

    Future<HttpServer> listen = vertx.createHttpServer().requestHandler(router).listen(0);
    listen.onSuccess(s -> {
      vertx.sharedData().getLocalMap("hy").put("metrics-port", s.actualPort());
      log.info(String.format("Metrics Web Server started. port: %s, %s, %s", s.actualPort(), userName, password));
      startPromise.complete();
    })
    .onFailure(err -> {
      log.error("Metrics Web Server start failed!", err);
      startPromise.fail(err);
    });

  }

  private static JsonObject pumpResponse(
    String endpointId, PumpStationControlResponseMessage response) {
    return new JsonObject()
      .put("success", true)
      .put("endpointId", endpointId)
      .put("telemetryStationAddress", response.telemetryStationAddress())
      .put("streamId", response.streamId())
      .put("reportTime", response.reportTime().toString())
      .put("length", response.length())
      .put("command", response.command());
  }

  private static String requiredText(JsonObject body, String field) {
    String value = body.getString(field);
    return value == null || value.trim().isEmpty() ? null : value.trim();
  }

  private static boolean validTimeout(Long timeout) {
    return timeout != null && timeout >= 1 && timeout <= 300;
  }

  private static void badRequest(io.vertx.ext.web.RoutingContext ctx, String message) {
    jsonError(ctx, 400, message);
  }

  private static void notFound(io.vertx.ext.web.RoutingContext ctx, String message) {
    jsonError(ctx, 404, message);
  }

  private static void downstreamFailure(io.vertx.ext.web.RoutingContext ctx, Throwable error) {
    Throwable cause = error;
    while (cause.getCause() != null && cause != cause.getCause()) {
      cause = cause.getCause();
    }
    int status = cause.getClass().getSimpleName().toLowerCase().contains("timeout") ? 504 :
      cause instanceof IllegalStateException ? 409 : 500;
    jsonError(ctx, status, cause.getMessage() == null ? cause.getClass().getSimpleName() : cause.getMessage());
  }

  private static void jsonError(io.vertx.ext.web.RoutingContext ctx, int status, String message) {
    ctx.response().setStatusCode(status)
      .putHeader("content-type", "application/json; charset=utf-8")
      .end(new JsonObject().put("success", false).put("error", message).encode());
  }
}
