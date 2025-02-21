package io.github.kukpt.sl651.codec;

import io.github.kukpt.sl651.utils.HydroLogicalUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.internal.ObjectUtil;

import static io.github.kukpt.sl651.utils.HydroLogicalUtils.*;

public class MessageHeader {

  public ByteBuf downstreamBuf(int bodyLen) {
    ByteBuf buf = Unpooled.buffer();
    buf.writeShort(FRAME_START_CHARACTER);
    byte[] tStationAddr = HydroLogicalUtils.strToBcd(telemetryStationAddress);
    buf.writeBytes(tStationAddr);
    buf.writeByte(centralStationAddress);
    buf.writeShort(password);
    buf.writeByte(functionType);
    buf.writeShort(0x8000 | bodyLen);
    buf.writeByte(STX);

    return buf;
  }

  public MessageHeader(short centralStationAddress,
                       String telemetryStationAddress,
                       int password,
                       int functionType,
                       int remainingLength,
                       short frameStart,
                       int totalLength,
                       int currentLength) {
    this.centralStationAddress = centralStationAddress;
    this.telemetryStationAddress = ObjectUtil.checkNotNull(telemetryStationAddress, "遥测站地址");
    this.password = password;
    this.functionType = functionType;
    this.remainingLength = remainingLength;
    this.frameStart = frameStart;
    this.totalPackage = totalLength;
    this.currentPackage = currentLength;
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
  private int functionType;

  /**
   * 报文长度
   */
  private int remainingLength;

  /**
   * 报文起始符
   */
  private short frameStart;

  /**
   * 包总数
   */
  private int totalPackage;

  /**
   * 当前包
   */
  private int currentPackage;

  public short centralStationAddress() {
    return centralStationAddress;
  }

  public String telemetryStationAddress() {
    return telemetryStationAddress;
  }

  public int password() {
    return password;
  }

  public int functionType() {
    return functionType;
  }

  public int remainingLength() {
    return remainingLength;
  }

  public short frameStart() {
    return frameStart;
  }

  /**
   * 多包报文
   * @return
   */
  public boolean multiPack() {
    return SYN == frameStart;
  }

  public boolean isLinkKeep() {
    return LINK_KEEP == functionType;
  }

  public int totalPackage() {
    return totalPackage;
  }

  public int currentPackage() {
    return currentPackage;
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
