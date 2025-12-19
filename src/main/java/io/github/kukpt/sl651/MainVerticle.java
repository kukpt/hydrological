package io.github.kukpt.sl651;

import io.github.kukpt.sl651.metrics.Endpoint;
import io.github.kukpt.sl651.metrics.MetricsStorage;
import io.github.kukpt.sl651.metrics.TrafficMonitor;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;

import java.util.ArrayList;
import java.util.List;

public class MainVerticle extends AbstractVerticle {

  public static void main(String[] args) {

    Vertx vertx = Vertx.vertx();

    vertx.deployVerticle(new MainVerticle())
    .onFailure(err -> err.printStackTrace());
  }


  @Override
  public void start(Promise<Void> startPromise) throws Exception {

    vertx.setPeriodic(5000L, id -> {
      List<TrafficMonitor> trafficMonitorQueueData = MetricsStorage.me().getTrafficMonitorQueueData("3120309009");
      System.err.println(trafficMonitorQueueData);
      ArrayList<Endpoint> values = MetricsStorage.me().values();
      System.err.println(values);
    });
    HydrologicalServer server = HydrologicalServer.create(vertx, new HydrologicalServerOptions().setEnableMetricsWeb(true));
    server.endpointHandler(ep -> {
//      ep.enableDebug();
      ep.messageHandler(msg -> {
        msg.header();
      });
      ep.closeHandler(p -> {
        System.err.println(p);
        ep.disableDebug();
      });
    });

    server.listen(11823)
    .onSuccess(s -> {
      System.out.println(s.actualPort());
    })
    .onFailure(err -> err.printStackTrace())
          .onComplete(unused -> startPromise.complete());

  }
}
