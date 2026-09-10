package io.github.kukpt.sl651;


import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;


public class Sl651MainVerticle extends AbstractVerticle {

  public static void main(String[] args) {

    Vertx vertx = Vertx.vertx();

    vertx.deployVerticle(new Sl651MainVerticle())
         .onFailure(err -> err.printStackTrace());
  }


  @Override
  public void start(Promise<Void> startPromise) throws Exception {

    Sl651Server server = Sl651Server.create(vertx, new Sl651ServerOptions()
                                                  .setMetricsWebUserName("admin")
                                                  .setMetricsWebPassword("123123123123")
    .enableMetricsWeb(true)
    .enableHttpProxy(true)
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
