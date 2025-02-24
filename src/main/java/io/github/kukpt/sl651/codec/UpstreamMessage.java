package io.github.kukpt.sl651.codec;


import io.github.kukpt.sl651.message.*;
import io.github.kukpt.sl651.utils.HydroLogicalUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.handler.codec.DecoderResult;
import io.vertx.core.Handler;

import static io.github.kukpt.sl651.utils.HydroLogicalUtils.ETB;

/**
 * @author shuo
 * 设备上行消息
 */
public class UpstreamMessage {

  private final MessageHeader header;

  private final HydrologicalPayload payload;

  private final DecoderResult coderResult;

  private final short frameEnd;

  private Handler<LinkKeepMessage> linkKeepMessageHandler;

  public UpstreamMessage linkKeepMessageHandler(Handler<LinkKeepMessage> handler) {
    this.linkKeepMessageHandler = handler;
    return this;
  }

  private Handler<TestMessage> testMessageHandler;

  public UpstreamMessage testMessageHandler(Handler<TestMessage> testMessageHandler) {
    this.testMessageHandler = testMessageHandler;
    return this;
  }

  private Handler<PeriodMessage> periodMessageHandler;

  public UpstreamMessage periodMessageHandler(Handler<PeriodMessage> periodMessageHandler) {
    this.periodMessageHandler = periodMessageHandler;
    return this;
  }

  private Handler<TimingMessage> timingMessageHandler;

  public UpstreamMessage timingMessageHandler(Handler<TimingMessage> timingMessageHandler) {
    this.timingMessageHandler = timingMessageHandler;
    return this;
  }

  private Handler<AdditionalMessage> additionalMessageHandler;

  public UpstreamMessage additionalMessageHandler(Handler<AdditionalMessage> additionalMessageHandler) {
    this.additionalMessageHandler = additionalMessageHandler;
    return this;
  }
  private Handler<HourlyMessage> hourlyMessageHandler;
  public UpstreamMessage hourlyMessageHandler(Handler<HourlyMessage> hourlyMessageHandler) {
    this.hourlyMessageHandler = hourlyMessageHandler;
    return this;
  }

  private Handler<PictureMessage> pictureMessageHandler;
  public UpstreamMessage pictureMessageHandler(Handler<PictureMessage> pictureMessageHandler) {
    this.pictureMessageHandler = pictureMessageHandler;
    return this;
  }

  private Handler<ByteBuf> byteBufHandler;
  public UpstreamMessage byteBufHandler(Handler<ByteBuf> byteBufHandler) {
    this.byteBufHandler = byteBufHandler;
    return this;
  }

  public void handle() {

    switch (this.functionType()) {
      case HydroLogicalUtils.LINK_KEEP:
          this.handleLinkKeepMessage(this.payload());
          break;
      case HydroLogicalUtils.TEST:
          this.handleTestMessage(this.payload());
          break;
        case HydroLogicalUtils.PERIOD:
          this.handlePeriodMessage(this.payload());
          break;
        case HydroLogicalUtils.TIMING:
          this.handlerTimingMessage(this.payload());
          break;
        case HydroLogicalUtils.ADDITIONAL:
          this.handleAdditionalMessage(this.payload());
          break;
        case HydroLogicalUtils.HOURLY:
          this.handleHourlyMessage(this.payload());
          break;
        case HydroLogicalUtils.PICTURE:
          this.handlePictureMessage(this.payload());
          break;
        default:
          this.handleByteBuf(this.payload());
          break;
      }

  }

  private void handleByteBuf(ByteBuf buffer) {
    if (this.byteBufHandler != null) {
      this.byteBufHandler.handle(buffer);
    }
  }
  private void handlePictureMessage(ByteBuf buffer) {
    if (this.pictureMessageHandler != null) {
      this.pictureMessageHandler.handle(new PictureMessage(buffer));
    }
  }

  private void handleLinkKeepMessage(ByteBuf payload) {
    if (this.linkKeepMessageHandler != null) {
      this.linkKeepMessageHandler.handle(new LinkKeepMessage(payload));
    }
  }

  private void handleHourlyMessage(ByteBuf payload) {
    if (this.hourlyMessageHandler != null) {
      this.hourlyMessageHandler.handle(new HourlyMessage(payload));
    }
  }

  private void handleAdditionalMessage(ByteBuf payload) {
    if (this.additionalMessageHandler != null) {
      this.additionalMessageHandler.handle(new AdditionalMessage(payload));
    }
  }

  private void handlerTimingMessage(ByteBuf payload) {
    if (this.timingMessageHandler != null) {
      this.timingMessageHandler.handle(new TimingMessage(payload));
    }
  }

  private void handlePeriodMessage(ByteBuf payload) {
    if (this.periodMessageHandler != null) {
      this.periodMessageHandler.handle(new PeriodMessage(payload));
    }
  }

  private void handleTestMessage(ByteBuf payload) {
    if (this.testMessageHandler != null) {
      this.testMessageHandler.handle(new TestMessage(payload));
    }
  }

  /**
   * 报文类型
   *
   * @return
   */
  public int functionType() {
    return header.functionType();
  }

  /**
   * 遥测站地址
   *
   * @return
   */
  public String telemetryStationAddress() {
    return header.telemetryStationAddress();
  }

  /**
   * 报文头
   *
   * @return
   */
  public MessageHeader header() {
    return header;
  }

  public MultiPack getMultiPack() {
    if (header().multiPack()) {
      return payload.mp();
    } else {
      throw new IllegalStateException("is single pack!");
    }
  }

  /**
   * 报文正文
   *
   * @return
   */
  public ByteBuf payload() {
    if (header().multiPack()) {
      return payload.mp().buffers();
    } else {
      return payload.sp();
    }
  }

  public DecoderResult coderResult() {
    return coderResult;
  }

  /**
   * @return
   */
  public boolean hasNextFrame() {
    return frameEnd == ETB;
  }

  public boolean checkDecoderResult() throws IllegalStateException {
    if (coderResult.isSuccess()) {
      return true;
    }
    throw new IllegalStateException("Hydrological Server decode error! " + coderResult.cause().getMessage());
  }


  UpstreamMessage(MessageHeader header, HydrologicalPayload payload, DecoderResult coderResult, short frameEnd) {
    this.header = header;
    this.payload = payload;
    this.coderResult = coderResult;
    this.frameEnd = frameEnd;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("HydrologicalMessage{");
    sb.append("header=").append(header);
    byte[] bs = new byte[this.payload().readableBytes()];
    this.payload().getBytes(0, bs);
    sb.append(", payload=").append(ByteBufUtil.hexDump(bs));
    sb.append('}');
    return sb.toString();
  }
}
