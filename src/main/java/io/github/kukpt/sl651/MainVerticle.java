package io.github.kukpt.sl651;


import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;


public class MainVerticle extends AbstractVerticle {

  public static void main(String[] args) {

    Vertx vertx = Vertx.vertx();

    vertx.deployVerticle(new MainVerticle())
    .onFailure(err -> err.printStackTrace());
  }


  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    String metricsUserName = System.getenv(HydrologicalServerOptions.METRICS_WEB_USERNAME_ENV);
    String metricsPassword = System.getenv(HydrologicalServerOptions.METRICS_WEB_PASSWORD_ENV);
    boolean enableMetricsWeb = !isBlank(metricsUserName) && !isBlank(metricsPassword);

    HydrologicalServer server = HydrologicalServer.create(vertx, new HydrologicalServerOptions()
        .enableMetricsWeb(enableMetricsWeb)
        .setMetricsLogBaseDir("/tmp/hy_logs"));
    server.endpointHandler(ep -> {
//      ep.enableDebug();
      ep.messageHandler(msg -> {
        msg.header();
      });
      ep.closeHandler(p -> {
        System.err.println(p);

      });
    });

    server.listen(11823)
    .onSuccess(s -> {
      System.out.println(s.actualPort());
    })
    .onFailure(err -> err.printStackTrace())
          .onComplete(unused -> startPromise.complete());

  }

  private static boolean isBlank(String value) {
    return value == null || value.trim().isEmpty();
  }
}
