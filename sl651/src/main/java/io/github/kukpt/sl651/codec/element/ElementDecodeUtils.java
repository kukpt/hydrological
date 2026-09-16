package io.github.kukpt.sl651.codec.element;

import io.netty.buffer.ByteBuf;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class ElementDecodeUtils {

  public static Collection<ElementResult> decodeDefaultElementResults(ByteBuf byteBuf) {
    final List<ElementResult> elementResults = new ArrayList<>();
    while (byteBuf.isReadable()) {
      final ElementId elementId = decodeElementId(byteBuf);
      elementResults.add(decodeElement(byteBuf, elementId));

    }
    return elementResults;
  }


  public static ElementResult decodeElement(ByteBuf byteBuf, ElementId elementId) {
    ElementValueType valueType = valueType(elementId);
    int expectedLength = valueLength(elementId);
    int rawValueLength = Math.min(expectedLength, byteBuf.readableBytes());
    byte[] rawValue = new byte[rawValueLength];
    byteBuf.readBytes(rawValue);

    // A truncated value cannot safely be interpreted as its declared type.
    if (rawValueLength < expectedLength) {
      valueType = ElementValueType.BYTE_ARRAY;
    }
    ElementResult result = new ElementResult(
      valueType, rawValue, elementId, rawValueLength);
    return canConvert(result)
      ? result
      : new ElementResult(
        ElementValueType.BYTE_ARRAY, rawValue, elementId, rawValueLength);
  }

  private static boolean canConvert(ElementResult result) {
    try {
      switch (result.valueType()) {
        case STRING:
          result.stringValue();
          break;
        case DOUBLE:
          result.doubleValue();
          break;
        case DOUBLE_ARRAY:
          result.doubleArrayValue();
          break;
        case TIME_STEP:
          result.timeStepValue();
          break;
        case STATUS:
          result.statusValue();
          break;
        case BYTE_ARRAY:
        default:
          break;
      }
      return true;
    } catch (RuntimeException ignored) {
      return false;
    }
  }

  private static ElementValueType valueType(ElementId elementId) {
    switch (elementId.id()) {
      // 观测时间、遥测站地址
      case 0xF0:
      case 0xF1:
        return ElementValueType.STRING;
      // 尚未支持的标准要素保留为原始字节
      case 0xF2:
      case 0xF3:
      case 0xFD:
      case 0x05:
        return ElementValueType.BYTE_ARRAY;
      // 1 小时内的雨量或水位序列
      case 0xF4:
      case 0xF5:
      case 0xF6:
      case 0xF7:
      case 0xF8:
      case 0xF9:
      case 0xFA:
      case 0xFB:
      case 0xFC:
        return ElementValueType.DOUBLE_ARRAY;
      // 时间步长码
      case 0x04:
        return ElementValueType.TIME_STEP;
      // 遥测站状态及报警信息
      case 0x45:
        return ElementValueType.STATUS;
      default:
        // 0xFFxx is a custom element identifier. Its semantics cannot be
        // inferred from the data definition, so preserve it as bytes.
        return elementId.id() > 0xFF
          ? ElementValueType.BYTE_ARRAY
          : ElementValueType.DOUBLE;
    }
  }

  private static int valueLength(ElementId elementId) {
    switch (elementId.id()) {
      case 0xF0:
      case 0xF1:
        return 5;
      case 0xF4:
        return 12;
      case 0xF5:
      case 0xF6:
      case 0xF7:
      case 0xF8:
      case 0xF9:
      case 0xFA:
      case 0xFB:
      case 0xFC:
        return 24;
      case 0x04:
        return 3;
      case 0x45:
        return 4;
      default:
        return elementId.readLength();
    }
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
