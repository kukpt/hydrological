package io.github.kukpt.sl651.codec;

import io.github.kukpt.sl651.utils.HydroLogicalUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.DecoderException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static io.github.kukpt.sl651.codec.FunctionType.LINK_KEEP;
import static io.github.kukpt.sl651.codec.FunctionType.PUMP_CONTROL;
import static io.github.kukpt.sl651.utils.HydroLogicalUtils.FRAME_START_CHARACTER;
import static io.github.kukpt.sl651.utils.HydroLogicalUtils.ETX;
import static io.github.kukpt.sl651.utils.HydroLogicalUtils.STX;


public class HydrologicalDecode extends ByteToMessageDecoder {


  enum DecoderState {
    HEADER,
    BODY,
    BAD_MESSAGE;

    private DecoderState() {
    }
  }

  public HydrologicalDecode() {

  }

  private MessageHeader header;

  private Integer remainingLength;

  @Override
  protected void decode(
  ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> out) throws Exception {
    try {
      if (!variableCrc16(byteBuf)) {
        throw new DecoderException("CRC16校验错误!");
      }
      header = decodeHeader(byteBuf);
      remainingLength = header.remainingLength();
      // 报文正文开始
      if (STX != byteBuf.readByte()) {
        throw new DecoderException("报文正文开始标识符错误!需要 (" + STX + ")");
      }
      // 链路维持报
      if (LINK_KEEP.equals(header.functionType())) {
        Result<LinkKeepMessage> result = decodeLinkKeep(byteBuf);
        remainingLength = remainingLength - result.numberOfBytesConsumed;
        HydrologicalMessage message = HydrologicalMessageFactory.newMessage(header, result.value);
        out.add(message);
      }
      // 泵站控制回复
      else if (PUMP_CONTROL.equals(header.functionType())) {
        Result<PumpStationControlResponseMessage> result = decodePumpControl(byteBuf);
        remainingLength = remainingLength - result.numberOfBytesConsumed;
        HydrologicalMessage message = HydrologicalMessageFactory.newMessage(header, result.value);
        out.add(message);
      }
      // 测试报，均匀时段水文信息报，遥测站定时报，遥测站加报报，遥测站小时报
      else {
        Result<FixedBodyMessage> fixedBodyMessageResult = decodeFixedBodyMessage(byteBuf);
        Result<?> result = decodeBody(header.functionType(), fixedBodyMessageResult.value, byteBuf);
        remainingLength = remainingLength - fixedBodyMessageResult.numberOfBytesConsumed - result.numberOfBytesConsumed;
        HydrologicalMessage message = HydrologicalMessageFactory.newMessage(header, result.value);
        out.add(message);
      }

      byteBuf.skipBytes(3); // 跳过 报文结束标识和CRC校验码
      if (remainingLength != 0) {
        throw new DecoderException(
        "non-zero remaining payload bytes: " +
        remainingLength + " (" + header.functionType() + ')');
      }

    } catch (Exception cause) {
      byteBuf.skipBytes(actualReadableBytes());
      out.add(invalidMessage(cause));
      throw new DecoderException(cause);
    }
  }

  private HydrologicalMessage invalidMessage(Throwable cause) {
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
    FunctionType ft = FunctionType.valueOf(funCode);
    // 长度
    short len = byteBuf.readShort();
    if (len < 0 || len > 4095) {
      throw new DecoderException("报文长度错误, 需要[1~4095],输入len= [" + len + "]");
    }
    return new MessageHeader(cAddr, tAddr, password, ft, len);
  }


  private static Result<?> decodeBody(FunctionType functionType, FixedBodyMessage fixedBodyMessage, ByteBuf byteBuf) {
    switch (functionType) {
      case TEST:
        return decodeTestMessage(byteBuf, fixedBodyMessage);
      case PERIOD:
        return decodePeriod(byteBuf, fixedBodyMessage);
      case TIMING:
        return decodeTiming(byteBuf, fixedBodyMessage);
      case ADDITIONAL:
        return decodeAdditional(byteBuf, fixedBodyMessage);
      case HOURLY:
        return decodeHourlyMessage(byteBuf, fixedBodyMessage);
      default:
        throw new DecoderException("Unknown message type, do not know how to validate the body");
    }
  }

  private static Result<LinkKeepMessage> decodeLinkKeep(ByteBuf byteBuf) {
    LinkKeepMessage linkKeepMessage = new LinkKeepMessage(byteBuf.readUnsignedShort(),
    HydroLogicalUtils.readReportTimeStr(byteBuf));
    return new Result<>(linkKeepMessage, 2 + 6);
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

  private static Result<TestMessage> decodeTestMessage(ByteBuf byteBuf, FixedBodyMessage fixedBodyMessage) {
    Result<Collection<ElementResult>> collectionResult = decodeDefaultElementResults(byteBuf);
    final TestMessage message = new TestMessage(fixedBodyMessage,
    collectionResult.value);
    return new Result<>(message, collectionResult.numberOfBytesConsumed);
  }

  private static Result<PeriodMessage> decodePeriod(ByteBuf byteBuf, FixedBodyMessage fixedBodyMessage) {

    byteBuf.skipBytes(2);
    TimeStep timeStep = TimeStep.createTimeStep(byteBuf);
    ElementId elementId = decodeElementId(byteBuf);
    int numberOfBytesConsumed = 5 + elementId.consumed();
    Collection<ElementResult> elementResults = new ArrayList<>();
    while (ETX != byteBuf.getByte(byteBuf.readerIndex())
    || byteBuf.readableBytes() > 3) {
      ElementResult elementResult = decodeElement(byteBuf, elementId);
      numberOfBytesConsumed += elementId.readLength();
      elementResults.add(elementResult);
    }
    PeriodMessage message = new PeriodMessage(fixedBodyMessage, timeStep, elementId, elementResults);
    return new Result<>(message, numberOfBytesConsumed);
  }

  private static Result<TimingMessage> decodeTiming(ByteBuf byteBuf, FixedBodyMessage fixedBodyMessage) {
    Result<Collection<ElementResult>> collectionResult = decodeDefaultElementResults(byteBuf);
    final TimingMessage message = new TimingMessage(fixedBodyMessage, collectionResult.value);
    return new Result<>(message, collectionResult.numberOfBytesConsumed);
  }

  private static Result<AdditionalMessage> decodeAdditional(ByteBuf byteBuf, FixedBodyMessage fixedBodyMessage) {
    Result<Collection<ElementResult>> collectionResult = decodeDefaultElementResults(byteBuf);
    final AdditionalMessage message = new AdditionalMessage(fixedBodyMessage, collectionResult.value);
    return new Result<>(message, collectionResult.numberOfBytesConsumed);
  }

  private static Result<HourlyMessage> decodeHourlyMessage(ByteBuf byteBuf, FixedBodyMessage fixedBodyMessage) {
    Result<Collection<ElementResult>> collectionResult = decodeDefaultElementResults(byteBuf);
    final HourlyMessage hourlyMessage = new HourlyMessage(fixedBodyMessage, collectionResult.value);
    return new Result<>(hourlyMessage, collectionResult.numberOfBytesConsumed);
  }

  private static Result<Collection<ElementResult>> decodeDefaultElementResults(ByteBuf byteBuf) {
    int numberOfBytesConsumed = 0;
    final List<ElementResult> elementResults = new ArrayList<>();
    while (ETX != byteBuf.getByte(byteBuf.readerIndex())
    || byteBuf.readableBytes() > 3) {
      final ElementId elementId = decodeElementId(byteBuf);
      final ElementResult elementResult = decodeElement(byteBuf, elementId);
      numberOfBytesConsumed =
      numberOfBytesConsumed + elementId.consumed() + elementResult.getNumberOfBytesConsumed();
      elementResults.add(elementResult);
    }
    return new Result<>(elementResults, numberOfBytesConsumed);
  }

  private static Result<FixedBodyMessage> decodeFixedBodyMessage(ByteBuf byteBuf) {
    int streamId = byteBuf.readUnsignedShort();// stream id
    ReportTime reportTime = HydroLogicalUtils.readReportTimeStr(byteBuf);
    String stationAddr = HydroLogicalUtils.readTelemetryStationAddressSkipElementId(byteBuf);
    short classificationCode = byteBuf.readUnsignedByte();// 分类码
    ObservationTime observationTime = HydroLogicalUtils.readObservationTimeSkipElementId(byteBuf);
    int numberOfBytesConsumed = 2 + 6 + 7 + 1 + 7;
    FixedBodyMessage fixedBodyMessage = new FixedBodyMessage(streamId, reportTime, stationAddr, classificationCode,
    observationTime);
    return new Result<>(fixedBodyMessage, numberOfBytesConsumed);
  }


  private static ElementResult decodeElement(ByteBuf byteBuf, ElementId elementId) {

    switch (elementId.id()) {
      // 观测时间
      case 0xF0:
        // value type String
        return new ElementResult(HydroLogicalUtils.readObservationTimeStr(byteBuf).toString(), elementId, 5);
      // 遥测站地址
      case 0xF1:
        // value type String
        return new ElementResult(HydroLogicalUtils.readTelemetryStationAddress(byteBuf).toString(), elementId, 5);
      case 0xF2:
        throw new DecoderException("the decoder is not supported [F2]");
      case 0xF3:
        throw new DecoderException("the decoder is not supported [F3]");
      case 0xF4:
        // 1 小时内每 5 分钟时段雨量
        //（每组雨量占 1 字节 HEX，最大值 25.4 毫米，数据中不含小数点；FFH 表示非法数据。）
        // value type byte[]
        if (byteBuf.readableBytes() < 12)
          throw new DecoderException();
        byte[] bytes = new byte[12];
        byteBuf.readBytes(bytes);
        return new ElementResult(bytes, elementId, 12);
      case 0xF5:
      case 0xF6:
      case 0xF7:
      case 0xF8:
      case 0xF9:
      case 0xFA:
      case 0xFB:
      case 0xFC:
        // 1 小时内 5 分钟间隔相对水位 1
        // (每组水位占 2 字节HEX，分辨力是为厘米，最大值为 655.34 米，数据中不含小数点；FFFFH 表示非法数据)；
        // 对于河道、闸坝（泵）站分别表示河道水位、闸（站）上水位
        // value type int[]
        if (byteBuf.readableBytes() < 2 * 12)
          throw new DecoderException();
        int[] values = new int[12];
        for (int i = 0; i < 12; i++) {
          values[i] = byteBuf.readUnsignedShort();
        }
        return new ElementResult(Arrays.toString(values), elementId, 24);
      case 0xFD:
        throw new DecoderException("the decoder is not supported [FD]");
      case 0x04:
        // 时间步长码
        // value type TimeStep
        TimeStep timeStep = TimeStep.createTimeStep(byteBuf);
        return new ElementResult(timeStep, elementId, 3);
      case 0x05:
        throw new DecoderException("the decoder is not supported [05]");
      case 0x45:
        // 遥测站状态及报警信息 4字节HEX
        if (byteBuf.readableBytes() < 4) {
          throw new DecoderException();
        }
        byte[] status = new byte[4];
        byteBuf.readBytes(status);
        return new ElementResult(status, elementId, 4);
      default:
        // value type double
        return new ElementResult(HydroLogicalUtils.readBcdNumber(byteBuf, elementId.readLength(), elementId.numberPoint()), elementId,
        elementId.readLength());
    }
  }

  private static ElementId decodeElementId(ByteBuf byteBuf) {
    int consumed = 0;
    // 要素标识
    short id = byteBuf.getUnsignedByte(byteBuf.readerIndex());
    int elementId;
    if (0xFF == id) {
      // 自定义要素
      elementId = byteBuf.readShort();
      consumed += 2;
    } else {
      elementId = byteBuf.readUnsignedByte();
      consumed += 1;
    }
    // 数据定义
    short dataDefinition = byteBuf.readUnsignedByte();
    consumed += 1;
    // 读取数据长度
    int readLength = (dataDefinition & 0xF8) >>> 3;
    // 小数点位数
    int numberPoint = dataDefinition & 0x7;
    return new ElementId(elementId, readLength, numberPoint, consumed);
  }

  private static boolean variableCrc16(ByteBuf buf) {
    int crcCode = buf.getUnsignedShort(buf.readableBytes() - 2);
    return crcCode == CRC16.crc16(buf, buf.readableBytes() - 2);
  }

}
