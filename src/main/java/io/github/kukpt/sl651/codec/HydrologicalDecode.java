package io.github.kukpt.sl651.codec;

import io.github.kukpt.sl651.codec.element.ElementId;
import io.github.kukpt.sl651.codec.element.ElementResult;
import io.github.kukpt.sl651.message.*;
import io.github.kukpt.sl651.utils.CRC16;
import io.github.kukpt.sl651.utils.HydroLogicalUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.DecoderException;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static io.github.kukpt.sl651.utils.HydroLogicalUtils.*;


public class HydrologicalDecode extends ByteToMessageDecoder {



  public HydrologicalDecode() {

  }

  private MessageHeader header;

  private MultiPack mp;

  private boolean isEtx(short frameEnd) {
    return ETX == frameEnd;
  }

  @Override
  protected void decode(
  ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> out) throws Exception {
    try {
      if (!variableCrc16(byteBuf)) {
        throw new DecoderException("CRC16校验错误!");
      }
      header = decodeHeader(byteBuf);
      int remainingLength = header.remainingLength();
      HydrologicalPayload hp;
      if (header.multiPack()) {
        mp = new MultiPack(header.totalPackage());
        ByteBuf payload = byteBuf.readBytes(remainingLength);
        mp.addPack(header.currentPackage(), payload);
        hp = new HydrologicalPayload(mp);
      } else {
        ByteBuf payload = byteBuf.readBytes(remainingLength);
        hp = new HydrologicalPayload(payload);
      }

      short frameEnd = byteBuf.readUnsignedByte();
      int crcCode = byteBuf.readUnsignedShort();

      if (isEtx(frameEnd)) {
        UpstreamMessage msg = HydrologicalMessageFactory.newMessage(header, hp, frameEnd, crcCode);
        out.add(msg);
      }


//      // 链路维持报
//      if (LINK_KEEP.equals(header.functionType())) {
//        Result<LinkKeepMessage> result = decodeLinkKeep(byteBuf);
//        remainingLength = remainingLength - result.numberOfBytesConsumed;
//        HydrologicalMessage message = HydrologicalMessageFactory.newMessage(header, result.value);
//        out.add(message);
//      }
//      // 泵站控制回复
//      else if (PUMP_CONTROL.equals(header.functionType())) {
//        Result<PumpStationControlResponseMessage> result = decodePumpControl(byteBuf);
//        remainingLength = remainingLength - result.numberOfBytesConsumed;
//        HydrologicalMessage message = HydrologicalMessageFactory.newMessage(header, result.value);
//        out.add(message);
//      }
//      // 测试报，均匀时段水文信息报，遥测站定时报，遥测站加报报，遥测站小时报
//      else {
//        Result<FixedBodyMessage> fixedBodyMessageResult = decodeFixedBodyMessage(byteBuf);
//        Result<?> result = decodeBody(header.functionType(), fixedBodyMessageResult.value, byteBuf);
//        remainingLength = remainingLength - fixedBodyMessageResult.numberOfBytesConsumed - result
//        .numberOfBytesConsumed;
//        HydrologicalMessage message = HydrologicalMessageFactory.newMessage(header, result.value);
//        out.add(message);
//      }
//
//      byteBuf.skipBytes(3); // 跳过 报文结束标识和CRC校验码
//      if (remainingLength != 0) {
//        throw new DecoderException(
//        "non-zero remaining payload bytes: " +
//        remainingLength + " (" + header.functionType() + ')');
//      }

    } catch (Exception cause) {
      byteBuf.skipBytes(actualReadableBytes());
      out.add(invalidMessage(cause));
      throw new DecoderException(cause);
    }
  }

  private UpstreamMessage invalidMessage(Throwable cause) {
    return HydrologicalMessageFactory.newInvalidMessage(header, cause);
  }

  private static final class Result<T> {

    private final T value;
    private final int numberOfBytesConsumed;

    Result(T value, int numberOfBytesConsumed) {
      this.value = value;
      this.numberOfBytesConsumed = numberOfBytesConsumed;
    }
  }

  private static MessageHeader decodeHeader(ByteBuf byteBuf) {
    // 桢起始符
    int start = byteBuf.readUnsignedShort();
    if (start != FRAME_START_CHARACTER) {
      throw new DecoderException("报文起始符错误, 需要[7E7E]传入->" + Integer.toHexString(start));
    }
    // 中心站地址
    short cAddr = byteBuf.readUnsignedByte();
    // 遥测站地址
    String tAddr = ByteBufUtil.hexDump(byteBuf, byteBuf.readerIndex(), 5);
    byteBuf.skipBytes(5);
    // 密码
    int password = byteBuf.readUnsignedShort();
    // 功能码
    int funCode = byteBuf.readUnsignedByte();
    // 长度
    short len = byteBuf.readShort();

    byte frameStart = byteBuf.readByte();
    if (frameStart == SYN) {
      // 多包传输正文开始
      byte[] bs = new byte[4];
      byteBuf.readBytes(bs, 1, 3);
      ByteBuffer bb = ByteBuffer.wrap(bs);
      int packages = bb.getInt();
      int currentPackage = packages & 0xFFF;
      int totalPackage = packages >>> 12;
      return new MessageHeader(cAddr, tAddr, password, funCode, len, frameStart, currentPackage, totalPackage);
    }
    return new MessageHeader(cAddr, tAddr, password, funCode, len, frameStart, 0, 0);
  }


  private static Result<?> decodeBody(FunctionType functionType, FixedBodyMessage fixedBodyMessage, ByteBuf byteBuf) {
    switch (functionType) {
//      case TEST:
//        return decodeTestMessage(byteBuf, fixedBodyMessage);
//      case PERIOD:
//        return decodePeriod(byteBuf, fixedBodyMessage);
//      case TIMING:
//        return decodeTiming(byteBuf, fixedBodyMessage);
//      case ADDITIONAL:
//        return decodeAdditional(byteBuf, fixedBodyMessage);
//      case HOURLY:
//        return decodeHourlyMessage(byteBuf, fixedBodyMessage);
      default:
        throw new DecoderException("Unknown message type, do not know how to validate the body");
    }
  }

  private static Result<PumpStationControlResponseMessage> decodePumpControl(ByteBuf byteBuf) {
    int streamId = byteBuf.readUnsignedShort();
    ReportTime reportTime = HydroLogicalUtils.readReportTimeStr(byteBuf);
    String telemetryStationAddress = HydroLogicalUtils.readTelemetryStationAddressSkipElementId(byteBuf);
    short length = byteBuf.readUnsignedByte();
    short command = byteBuf.readUnsignedByte();
    int numberOfBytesConsumed = 2 + 6 + 7 + 1 + 1;
    PumpStationControlResponseMessage message =
    new PumpStationControlResponseMessage(streamId, reportTime, telemetryStationAddress, length, command);
    return new Result<>(message, numberOfBytesConsumed);
  }

  private static boolean variableCrc16(ByteBuf buf) {
    int crcCode = buf.getUnsignedShort(buf.readableBytes() - 2);
    return crcCode == CRC16.crc16(buf, buf.readableBytes() - 2);
  }

}
