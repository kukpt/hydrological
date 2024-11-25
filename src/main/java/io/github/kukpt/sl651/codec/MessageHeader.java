package io.github.kukpt.sl651.codec;

import io.github.kukpt.sl651.utils.HydroLogicalUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.internal.ObjectUtil;

import static io.github.kukpt.sl651.utils.HydroLogicalUtils.FRAME_START_CHARACTER;
import static io.github.kukpt.sl651.utils.HydroLogicalUtils.STX;

public class MessageHeader {

  public ByteBuf downstreamBuf(int bodyLen) {
    ByteBuf buf = Unpooled.buffer();
    buf.writeShort(FRAME_START_CHARACTER);
    byte[] tStationAddr = HydroLogicalUtils.strToBcd(telemetryStationAddress);
    buf.writeBytes(tStationAddr);
    buf.writeByte(centralStationAddress);
    buf.writeShort(password);
    buf.writeByte(functionType().value());
    buf.writeShort(0x8000 | bodyLen);
    buf.writeByte(STX);

    return buf;
  }

  public MessageHeader(short centralStationAddress,
                       String telemetryStationAddress,
                       int password,
                       FunctionType functionType,
                       int remainingLength) {
    this.centralStationAddress = centralStationAddress;
    this.telemetryStationAddress = ObjectUtil.checkNotNull(telemetryStationAddress, "遥测站地址");
    this.password = password;
    this.functionType = ObjectUtil.checkNotNull(functionType, "functionType");
    this.remainingLength = remainingLength;
  }

  /**
   * 中心站地址
   */
  private short centralStationAddress;

  /**
   * 遥测站地址
   */
  private String telemetryStationAddress;

  /**
   * 密码
   */
  private int password;

  /**
   * 功能代码
   */
  private FunctionType functionType;

  /**
   * 报文长度
   */
  private int remainingLength;

  public short centralStationAddress() {
    return centralStationAddress;
  }

  public String telemetryStationAddress() {
    return telemetryStationAddress;
  }

  public int password() {
    return password;
  }

  public FunctionType functionType() {
    return functionType;
  }

  public int remainingLength() {
    return remainingLength;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("MessageHeader{");
    sb.append("centralStationAddress=").append(centralStationAddress);
    sb.append(", telemetryStationAddress='").append(telemetryStationAddress).append('\'');
    sb.append(", password=").append(password);
    sb.append(", functionType=").append(functionType);
    sb.append('}');
    return sb.toString();
  }
}
