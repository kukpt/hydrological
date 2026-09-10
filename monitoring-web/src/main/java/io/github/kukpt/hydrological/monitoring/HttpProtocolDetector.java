package io.github.kukpt.hydrological.monitoring;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.vertx.core.Vertx;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

/** Splits HTTP and binary protocol connections sharing one TCP listening port. */
public class HttpProtocolDetector extends ByteToMessageDecoder {
  private static final List<String> HTTP_PREFIXES = Arrays.asList(
    "GET ", "POST ", "PUT ", "PATCH ", "DELETE ", "HEAD ", "OPTIONS ", "CONNECT ", "TRACE ", "PRI ");

  private final Vertx vertx;
  private final Consumer<ChannelPipeline> protocolPipelineInitializer;

  public HttpProtocolDetector(Vertx vertx, Consumer<ChannelPipeline> protocolPipelineInitializer) {
    this.vertx = vertx;
    this.protocolPipelineInitializer = protocolPipelineInitializer;
  }

  @Override
  protected void decode(ChannelHandlerContext ctx, ByteBuf input, List<Object> out) {
    Detection detection = detect(input);
    if (detection == Detection.NEED_MORE) return;
    if (detection == Detection.HTTP) {
      Integer port = MonitoringWebProxyTarget.port(vertx);
      String host = MonitoringWebProxyTarget.host(vertx);
      if (port == null || host == null) {
        ctx.fireExceptionCaught(new IllegalStateException(
          "HTTP proxy is enabled but the shared monitoring Web is not running"));
        ctx.close();
        return;
      }
      ctx.pipeline().addBefore("handler", "monitoring-http-proxy",
        new LocalTcpToHttpProxyHandler(vertx, host, port));
      ctx.pipeline().remove("handler");
    } else {
      protocolPipelineInitializer.accept(ctx.pipeline());
    }
    ctx.pipeline().remove(this);
  }

  static boolean isHttp(ByteBuf input) {
    return detect(input) == Detection.HTTP;
  }

  private static Detection detect(ByteBuf input) {
    int length = Math.min(input.readableBytes(), 9);
    if (length == 0) return Detection.NEED_MORE;
    byte[] bytes = new byte[length];
    input.getBytes(input.readerIndex(), bytes);
    String prefix = new String(bytes, StandardCharsets.US_ASCII).toUpperCase();
    boolean possibleHttp = false;
    for (String method : HTTP_PREFIXES) {
      if (prefix.startsWith(method)) return Detection.HTTP;
      if (method.startsWith(prefix)) possibleHttp = true;
    }
    return possibleHttp ? Detection.NEED_MORE : Detection.PROTOCOL;
  }

  private enum Detection { HTTP, PROTOCOL, NEED_MORE }
}
