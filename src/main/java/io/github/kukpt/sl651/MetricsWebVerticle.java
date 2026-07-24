package io.github.kukpt.sl651;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.github.kukpt.sl651.utils.LocalEbTopic;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
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


  public MetricsWebVerticle(String userName, String password) {
    this.userName = userName;
    this.password = password;
  }

  static {
    DatabindCodec.mapper().registerModule(new JavaTimeModule());
    DatabindCodec.mapper().disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
  }

  @Override
  public void start() {

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
    router.get("/wisetion").handler(ctx -> ctx.redirect("/static/login.html"));

    Future<HttpServer> listen = vertx.createHttpServer().requestHandler(router).listen(0);
    listen.onSuccess(s -> {
      vertx.sharedData().getLocalMap("hy").put("metrics-port", s.actualPort());
      log.info(String.format("Metrics Web Server started. port: %s", s.actualPort()));
    })
    .onFailure(err -> {
      log.error("Metrics Web Server start failed!", err);
    });

  }
}
