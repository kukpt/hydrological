package io.github.kukpt.sl651.metrics;

import io.github.kukpt.sl651.HydrologicalEndpoint;
import io.github.kukpt.sl651.utils.LocalEbTopic;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.vertx.core.Vertx;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;

import java.util.Queue;

public class TrafficMonitorHandler extends ChannelDuplexHandler {

  private static final Logger log = LoggerFactory.getLogger(TrafficMonitorHandler.class);

  private final Vertx vertx;

  private final HydrologicalEndpoint endpoint;

  public TrafficMonitorHandler(Vertx vertx, HydrologicalEndpoint endpoint) {
    this.vertx = vertx;
    this.endpoint = endpoint;
  }

  private void record(TrafficMonitor.TYPE type, ByteBuf msg) {
    TrafficMonitor trafficMonitor = new TrafficMonitor(type, this.endpoint, ByteBufUtil.hexDump(msg));
    vertx.eventBus().request(LocalEbTopic.endpointMessageAppendTopic(), trafficMonitor.toJson())
        .onFailure(err -> {
          log.error(err);
        });
  }

  // 监控入站数据 (Read)
  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    if (msg instanceof ByteBuf) {
      record(TrafficMonitor.TYPE.INBOUND, (ByteBuf) msg);
    }
    super.channelRead(ctx, msg);
  }

  // 监控出站数据 (Write)
  @Override
  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
    if (msg instanceof ByteBuf) {
      record(TrafficMonitor.TYPE.OUTBOUND, (ByteBuf) msg);
    }
    super.write(ctx, msg, promise);
  }

}
