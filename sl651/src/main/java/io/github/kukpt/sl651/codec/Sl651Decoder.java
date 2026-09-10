package io.github.kukpt.sl651.codec;

import io.github.kukpt.hydrological.common.ProtocolDecodeLogger;
import io.github.kukpt.hydrological.common.ProtocolTrafficRecord;
import io.github.kukpt.hydrological.common.ProtocolTrafficRecorder;
import io.github.kukpt.hydrological.common.ProtocolType;
import io.github.kukpt.hydrological.common.TrafficDirection;
import io.github.kukpt.sl651.utils.CRC16;
import io.github.kukpt.sl651.utils.Sl651CodecUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.DecoderException;
import io.vertx.core.json.JsonObject;

import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.List;

import static io.github.kukpt.sl651.utils.Sl651CodecUtils.*;


public class Sl651Decoder extends ByteToMessageDecoder {

  private static final ProtocolDecodeLogger log =
    ProtocolDecodeLogger.getLogger(Sl651Decoder.class, ProtocolType.SL651);

  public Sl651Decoder() {
    this(ProtocolTrafficRecorder.NOOP, false);
  }

  public Sl651Decoder(ProtocolTrafficRecorder trafficRecorder, boolean includeParsedData) {
    this.trafficRecorder = trafficRecorder == null ? ProtocolTrafficRecorder.NOOP : trafficRecorder;
    this.includeParsedData = includeParsedData;
  }

  private final ProtocolTrafficRecorder trafficRecorder;
  private final boolean includeParsedData;

  private MessageHeader header;

  private MultiPack mp;

  private boolean isEtx(short frameEnd) {
    return ETX == frameEnd;
  }

  @Override
  protected void decode(
  ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> out) throws Exception {
    byte[] raw = ByteBufUtil.getBytes(byteBuf, byteBuf.readerIndex(), byteBuf.readableBytes());
    header = null;
    boolean recorded = false;
    log.debugRaw("frame", "开始解码, bytes=" + raw.length, raw);
    try {
      if (!variableCrc16(byteBuf)) {
        throw new DecoderException("CRC16校验错误!");
      }
      log.debug("checksum", "CRC16校验通过");
      header = decodeHeader(byteBuf);
      log.debug("header", "报文头解析完成: " + header);
      int remainingLength = header.remainingLength();
      Sl651Payload hp;
      if (header.multiPack()) {
        if (header.currentPackage() == 1) {
          mp = new MultiPack(header.totalPackage());
        }
        ByteBuf payload = byteBuf.readBytes(remainingLength);
        if (mp == null) {
          mp = new MultiPack(header.totalPackage());
        }
        mp.addPack(header.currentPackage(), payload);
        log.debug("assembly", "接收分包 " + header.currentPackage() + '/' + header.totalPackage()
          + ", payloadBytes=" + payload.readableBytes());
        hp = new Sl651Payload(mp);
      } else {
        ByteBuf payload = byteBuf.readBytes(remainingLength);
        log.debug("payload", "正文读取完成, payloadBytes=" + payload.readableBytes());
        hp = new Sl651Payload(payload);
      }

      short frameEnd = byteBuf.readUnsignedByte();
      int crcCode = byteBuf.readUnsignedShort();

      if (isEtx(frameEnd)) {
        UpstreamMessage msg = Sl651MessageFactory.newMessage(header, hp, frameEnd, crcCode);
        JsonObject parsedData = null;
        String parseStatus = "SUCCESS";
        String parseError = null;
        if (includeParsedData) {
          try {
            parsedData = msg.functionType() == Sl651CodecUtils.PICTURE
              ? Sl651ParsedDataJson.encodePictureMetadata(msg)
              : Sl651ParsedDataJson.encode(msg, msg.parseBody());
          } catch (RuntimeException error) {
            // Monitoring enrichment must never change whether the protocol message is delivered.
            parseStatus = "INVALID_DATA";
            parseError = safeMessage(error);
          }
        }
        recordTraffic(channelHandlerContext, raw, parseStatus, parsedData, parseError);
        recorded = true;
        out.add(msg);
        log.debug("complete", "解码完成, functionCode=0x" + Integer.toHexString(header.functionType()).toUpperCase());
      } else {
        recordTraffic(channelHandlerContext, raw, "PARTIAL", null, null);
        recorded = true;
        log.debug("frame-end", "当前帧结束符不是ETX，未生成上行消息, frameEnd=0x"
          + Integer.toHexString(frameEnd).toUpperCase());
      }

    } catch (Exception cause) {
      log.error("failure", "解码失败, raw=" + ProtocolDecodeLogger.hex(raw), cause);
      if (!recorded) {
        recordTraffic(channelHandlerContext, raw, "INVALID_FRAME", null, safeMessage(cause));
      }
      byteBuf.skipBytes(actualReadableBytes());
      out.add(invalidMessage(cause));
      throw new DecoderException(cause);
    }
  }

  private void recordTraffic(ChannelHandlerContext context, byte[] raw, String parseStatus,
      JsonObject parsedData, String parseError) {
    if (trafficRecorder == ProtocolTrafficRecorder.NOOP) return;
    try {
      SocketAddress remote = context.channel().remoteAddress();
      String remoteAddress = remote == null ? null : remote.toString();
      String endpointId = header == null ? "SL651:" + String.valueOf(remoteAddress)
        : header.telemetryStationAddress();
      trafficRecorder.record(new ProtocolTrafficRecord(ProtocolType.SL651,
        TrafficDirection.INBOUND, endpointId, remoteAddress,
        ProtocolDecodeLogger.fullHex(raw), parseStatus, parsedData, parseError));
    } catch (RuntimeException error) {
      log.error("record", "监控日志记录失败", error);
    }
  }

  private static String safeMessage(Throwable error) {
    String message = error.getMessage();
    if (message == null) message = error.getClass().getSimpleName();
    return message.length() > 512 ? message.substring(0, 512) : message;
  }

  private UpstreamMessage invalidMessage(Throwable cause) {
    return Sl651MessageFactory.newInvalidMessage(header, cause);
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
      return new MessageHeader(cAddr, tAddr, password, funCode, len - 3, frameStart, currentPackage, totalPackage);
    }
    return new MessageHeader(cAddr, tAddr, password, funCode, len, frameStart, 0, 0);
  }

  private static Result<PumpStationControlResponseMessage> decodePumpControl(ByteBuf byteBuf) {
    int streamId = byteBuf.readUnsignedShort();
    ReportTime reportTime = Sl651CodecUtils.readReportTimeStr(byteBuf);
    String telemetryStationAddress = Sl651CodecUtils.readTelemetryStationAddressSkipElementId(byteBuf);
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
