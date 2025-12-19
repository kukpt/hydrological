package io.github.kukpt.sl651.metrics;

import io.github.kukpt.sl651.HydrologicalEndpoint;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

import java.util.Queue;

public class TrafficMonitorHandler extends ChannelDuplexHandler {

  private final HydrologicalEndpoint endpoint;

  private final Queue<TrafficMonitor> queue;

  public TrafficMonitorHandler(HydrologicalEndpoint endpoint, Queue<TrafficMonitor> queue) {
    this.endpoint = endpoint;
    this.queue = queue;
  }

  // 监控入站数据 (Read)
  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    if (msg instanceof ByteBuf) {
      TrafficMonitor trafficMonitor = new TrafficMonitor(TrafficMonitor.TYPE.INBOUND, this.endpoint,
                                                         ByteBufUtil.hexDump((ByteBuf) msg));
      queue.add(trafficMonitor);
    }
    super.channelRead(ctx, msg);
  }

  // 监控出站数据 (Write)
  @Override
  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
    if (msg instanceof ByteBuf) {
      TrafficMonitor trafficMonitor = new TrafficMonitor(TrafficMonitor.TYPE.OUTBOUND, this.endpoint,
                                                         ByteBufUtil.hexDump((ByteBuf) msg));
      queue.add(trafficMonitor);
    }
    super.write(ctx, msg, promise);
  }

}
