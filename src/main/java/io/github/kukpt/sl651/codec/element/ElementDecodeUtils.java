package io.github.kukpt.sl651.codec.element;

import io.github.kukpt.sl651.codec.TimeStep;
import io.github.kukpt.sl651.utils.HydroLogicalUtils;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static io.github.kukpt.sl651.utils.HydroLogicalUtils.ptn;


public final class ElementDecodeUtils {

  public static Collection<ElementResult> decodeDefaultElementResults(ByteBuf byteBuf) {
    int numberOfBytesConsumed = 0;
    final List<ElementResult> elementResults = new ArrayList<>();
    while (byteBuf.isReadable()) {
      final ElementId elementId = decodeElementId(byteBuf);
      final ElementResult elementResult = decodeElement(byteBuf, elementId);
      numberOfBytesConsumed =
      numberOfBytesConsumed + elementId.consumed() + elementResult.getNumberOfBytesConsumed();
      elementResults.add(elementResult);
    }
    return elementResults;
  }


  public static ElementResult decodeElement(ByteBuf byteBuf, ElementId elementId) {

    switch (elementId.id()) {
      // 观测时间
      case 0xF0:
        // value type String
        return new ElementResult(ElementValueType.STRING, HydroLogicalUtils.readObservationTimeStr(byteBuf).toString(), elementId, 5);
      // 遥测站地址
      case 0xF1:
        // value type String
        return new ElementResult(ElementValueType.STRING, HydroLogicalUtils.readTelemetryStationAddress(byteBuf).toString(), elementId, 5);
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
        int[] f4Value = new int[12];
        for (int i = 0; i < 12; i++) {
          f4Value[i] = byteBuf.readUnsignedByte();
        }
        return new ElementResult(ElementValueType.DOUBLE_ARRAY, toDoubleArray(f4Value, 1), elementId, 12);
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
        return new ElementResult(ElementValueType.DOUBLE_ARRAY, toDoubleArray(values, 2), elementId, 24);
      case 0xFD:
        throw new DecoderException("the decoder is not supported [FD]");
      case 0x04:
        // 时间步长码
        // value type TimeStep
        TimeStep timeStep = TimeStep.createTimeStep(byteBuf);
        return new ElementResult(ElementValueType.TIME_STEP, timeStep, elementId, 3);
      case 0x05:
        throw new DecoderException("the decoder is not supported [05]");
      case 0x45:
        // 遥测站状态及报警信息 4字节HEX
        Long status = byteBuf.readUnsignedInt();
        return new ElementResult(ElementValueType.STATUS, status, elementId, 4);
      default:
        // value type double
        return new ElementResult(ElementValueType.DOUBLE,
        HydroLogicalUtils.readBcdNumber(byteBuf, elementId.readLength(), elementId.numberPoint()), elementId,
        elementId.readLength());
    }
  }

  private static double[]  toDoubleArray(int[] values, int numberPoint) {
    double[] doubles = new double[values.length];
    for (int i = 0; i < values.length; i++) {
      if (values[i] > 0) {
        doubles[i] = (double) values[i] / HydroLogicalUtils.ptn(numberPoint);
      } else {
        doubles[i] = 0;
      }
    }
    return doubles;
  }

  public static ElementId decodeElementId(ByteBuf byteBuf) {
    int consumed = 0;
    // 要素标识
    short id = byteBuf.getUnsignedByte(byteBuf.readerIndex());
    int elementId;
    if (0xFF == id) {
      // 自定义要素
      elementId = byteBuf.readShort();
      elementId = elementId ^ 0xFFFF0000;
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

}
