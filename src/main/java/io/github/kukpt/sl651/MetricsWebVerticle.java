package io.github.kukpt.sl651;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.github.kukpt.sl651.metrics.Endpoint;
import io.github.kukpt.sl651.metrics.MetricsStorage;
import io.github.kukpt.sl651.metrics.TrafficMonitor;
import io.github.kukpt.sl651.utils.LocalEbTopic;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.internal.logging.Logger;
import io.vertx.core.internal.logging.LoggerFactory;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.jackson.DatabindCodec;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.authentication.AuthenticationProvider;
import io.vertx.ext.auth.authentication.UsernamePasswordCredentials;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.*;
import io.vertx.ext.web.sstore.LocalSessionStore;

import java.util.ArrayList;
import java.util.List;

public class MetricsWebVerticle extends AbstractVerticle {

  private static final Logger log = LoggerFactory.getLogger(MetricsWebVerticle.class);

  private final String userName;

  private final String password;

  private final int metricsWebPort;

  public MetricsWebVerticle(String userName, String password, int metricsWebPort) {
    this.userName = userName;
    this.password = password;
    this.metricsWebPort = metricsWebPort;
  }

  static {
    DatabindCodec.mapper().registerModule(new JavaTimeModule());
    DatabindCodec.mapper().disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
  }

  @Override
  public void start() {
    AuthenticationProvider authProvider = credentials -> {
      UsernamePasswordCredentials unc = (UsernamePasswordCredentials) credentials;

      if (this.userName.equals(unc.getUsername()) && this.password.equals(unc.getPassword())) {
        return io.vertx.core.Future.succeededFuture(
        User.create(new JsonObject().put("username", unc.getUsername())));
      } else {
        return Future.failedFuture("Invalid credentials");
      }
    };
    Router router = Router.router(vertx);

    router.route().handler(BodyHandler.create());

    router.route().handler(SessionHandler.create(LocalSessionStore.create(vertx)));
    AuthenticationHandler rh = RedirectAuthHandler.create(authProvider, "/static/login.html");


    router.route("/static/*").handler(StaticHandler.create("webroot"));

    router.post("/do-login").handler(FormLoginHandler.create(authProvider).setDirectLoggedInOKURL("/private/dashboard.html"));

    // --- 4. 路由保护拦截器 ---
    // 所有访问 /private/* 的请求，如果没有登录，自动重定向到 /static/login.html
    router.route("/private/*").handler(rh);

    // 映射受保护的静态资源（把受保护的 HTML 放在另一个路径下）
    router.route("/private/*").handler(StaticHandler.create("webroot/private"));

    router.get("/private/connections").handler(ctx -> {
      ArrayList<Endpoint> values = MetricsStorage.me().values();
      ctx.json(values);
    });

    router.get("/private/trafficMonitor").handler(ctx -> {
      String endpointId = ctx.request().getParam("endpointId");
      List<TrafficMonitor> trafficMonitorQueueData = MetricsStorage.me().getTrafficMonitorQueueData(endpointId);
      ctx.json(trafficMonitorQueueData);
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
      ctx.userContext().clear();
      ctx.redirect("/static/login.html");
    });

    // 根目录自动跳转到登录
    router.get("/").handler(ctx -> ctx.redirect("/static/login.html"));

    vertx.createHttpServer().requestHandler(router).listen(metricsWebPort);
    log.info(String.format("Metrics Web Server started. port: %s", metricsWebPort));
  }
}
