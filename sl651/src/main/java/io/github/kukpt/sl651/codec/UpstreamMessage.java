package io.github.kukpt.sl651.codec;


import io.github.kukpt.sl651.message.*;
import io.github.kukpt.sl651.utils.Sl651CodecUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.handler.codec.DecoderResult;
import io.vertx.core.Handler;

import static io.github.kukpt.sl651.utils.Sl651CodecUtils.ETB;

/**
 * @author shuo
 * 设备上行消息
 */
public class UpstreamMessage {

  /**
   * 报文正文
   */
  private IMessageBody messageBody;

  private final MessageHeader header;

  private final Sl651Payload payload;

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

  private Handler<PumpStationControlResponseMessage> pumpStationControlResponseHandler;
  public UpstreamMessage pumpStationControlResponseHandler(
    Handler<PumpStationControlResponseMessage> pumpStationControlResponseHandler) {
    this.pumpStationControlResponseHandler = pumpStationControlResponseHandler;
    return this;
  }

  private Handler<ByteBuf> byteBufHandler;
  public UpstreamMessage byteBufHandler(Handler<ByteBuf> byteBufHandler) {
    this.byteBufHandler = byteBufHandler;
    return this;
  }

  public void handle() {
    IMessageBody body = parseBody();
    switch (this.functionType()) {
      case Sl651CodecUtils.LINK_KEEP:
          if (this.linkKeepMessageHandler != null) {
            this.linkKeepMessageHandler.handle((LinkKeepMessage) body);
          }
          break;
      case Sl651CodecUtils.TEST:
          if (this.testMessageHandler != null) this.testMessageHandler.handle((TestMessage) body);
          break;
        case Sl651CodecUtils.PERIOD:
          if (this.periodMessageHandler != null) this.periodMessageHandler.handle((PeriodMessage) body);
          break;
        case Sl651CodecUtils.TIMING:
          if (this.timingMessageHandler != null) this.timingMessageHandler.handle((TimingMessage) body);
          break;
        case Sl651CodecUtils.ADDITIONAL:
          if (this.additionalMessageHandler != null) this.additionalMessageHandler.handle((AdditionalMessage) body);
          break;
        case Sl651CodecUtils.HOURLY:
          if (this.hourlyMessageHandler != null) this.hourlyMessageHandler.handle((HourlyMessage) body);
          break;
        case Sl651CodecUtils.PICTURE:
          if (this.pictureMessageHandler != null) this.pictureMessageHandler.handle((PictureMessage) body);
          break;
        case 0x4C:
          if (this.pumpStationControlResponseHandler != null) {
            this.pumpStationControlResponseHandler.handle((PumpStationControlResponseMessage) body);
          }
          break;
        default:
          this.handleByteBuf(this.payload());
          break;
      }

  }

  /** Parses the body once without invoking any application handlers. */
  public synchronized IMessageBody parseBody() {
    if (messageBody != null) return messageBody;
    ByteBuf source = payload();
    if (source == null) return null;
    ByteBuf body = source.duplicate();
    switch (functionType()) {
      case Sl651CodecUtils.LINK_KEEP:
        messageBody = new LinkKeepMessage(body);
        break;
      case Sl651CodecUtils.TEST:
        messageBody = new TestMessage(body);
        break;
      case Sl651CodecUtils.PERIOD:
        messageBody = new PeriodMessage(body);
        break;
      case Sl651CodecUtils.TIMING:
        messageBody = new TimingMessage(body);
        break;
      case Sl651CodecUtils.ADDITIONAL:
        messageBody = new AdditionalMessage(body);
        break;
      case Sl651CodecUtils.HOURLY:
        messageBody = new HourlyMessage(body);
        break;
      case Sl651CodecUtils.PICTURE:
        messageBody = new PictureMessage(body);
        break;
      case 0x4C:
        messageBody = new PumpStationControlResponseMessage(body);
        break;
      default:
        return null;
    }
    return messageBody;
  }

  private void handleByteBuf(ByteBuf buffer) {
    if (this.byteBufHandler != null) {
      this.byteBufHandler.handle(buffer);
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
    throw new IllegalStateException("SL651 server decode error! " + coderResult.cause().getMessage());
  }


  UpstreamMessage(MessageHeader header, Sl651Payload payload, DecoderResult coderResult, short frameEnd) {
    this.header = header;
    this.payload = payload;
    this.coderResult = coderResult;
    this.frameEnd = frameEnd;
  }

  public int streamId() {
    if (this.messageBody != null) {
      return this.messageBody.streamId();
    }
    ByteBuf body = this.payload();
    if (body != null && body.readableBytes() >= 2) {
      return body.getUnsignedShort(body.readerIndex());
    }
    return 0;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("Sl651Message{");
    sb.append("header=").append(header);
    byte[] bs = new byte[this.payload().readableBytes()];
    this.payload().getBytes(0, bs);
    sb.append(", payload=").append(ByteBufUtil.hexDump(bs));
    sb.append('}');
    return sb.toString();
  }
}
