package io.github.kukpt.sl651.metrics;

import io.github.kukpt.hydrological.common.EventBusProtocolTrafficRecorder;
import io.github.kukpt.hydrological.common.ProtocolTrafficRecord;
import io.github.kukpt.hydrological.common.ProtocolTrafficRecorder;
import io.github.kukpt.hydrological.common.ProtocolType;
import io.github.kukpt.hydrological.common.TrafficDirection;
import io.github.kukpt.sl651.Sl651Endpoint;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.vertx.core.Vertx;

public class TrafficMonitorHandler extends ChannelDuplexHandler {

  private final Sl651Endpoint endpoint;
  private final ProtocolTrafficRecorder recorder;

  public TrafficMonitorHandler(Vertx vertx, Sl651Endpoint endpoint) {
    this.endpoint = endpoint;
    this.recorder = new EventBusProtocolTrafficRecorder(vertx);
  }

  private void record(TrafficDirection type, ByteBuf msg) {
    recorder.record(new ProtocolTrafficRecord(ProtocolType.SL651,
      type, endpoint.endpointId(), endpoint.remoteAddress().toString(),
      ByteBufUtil.hexDump(msg)));
  }

  // Inbound frames are recorded by Sl651Decoder together with their parse result.
  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    super.channelRead(ctx, msg);
  }

  // 监控出站数据 (Write)
  @Override
  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
    if (msg instanceof ByteBuf) {
      record(TrafficDirection.OUTBOUND, (ByteBuf) msg);
    }
    super.write(ctx, msg, promise);
  }

}
