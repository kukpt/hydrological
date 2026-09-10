package io.github.kukpt.sl427.codec;

import io.github.kukpt.hydrological.common.ProtocolDecodeLogger;
import io.github.kukpt.hydrological.common.ProtocolType;
import io.vertx.core.buffer.Buffer;

import java.util.ArrayList;
import java.util.List;

/** Extracts ordinary SL427 frames from a TCP byte stream. Image-frame length extensions are not supported. */
public final class Sl427StreamDecoder {

  private static final ProtocolDecodeLogger log =
    ProtocolDecodeLogger.getLogger(Sl427StreamDecoder.class, ProtocolType.SL427);

  private static final int START = 0x68;
  private static final int MIN_USER_DATA_LENGTH = 7;

  private final int maxBufferedBytes;
  private Buffer cumulative = Buffer.buffer();

  public Sl427StreamDecoder() {
    this(64 * 1024);
  }

  public Sl427StreamDecoder(int maxBufferedBytes) {
    if (maxBufferedBytes < 260) {
      throw new IllegalArgumentException("maxBufferedBytes must be at least 260");
    }
    this.maxBufferedBytes = maxBufferedBytes;
  }

  public synchronized List<byte[]> decode(Buffer incoming) {
    if (incoming != null && incoming.length() > 0) {
      log.debugRaw("stream", "收到TCP数据块, chunkBytes=" + incoming.length()
        + ", bufferedBefore=" + cumulative.length(), incoming.getBytes());
      cumulative.appendBuffer(incoming);
    }
    if (cumulative.length() > maxBufferedBytes) {
      log.warn("stream", "接收缓冲区超限, bufferedBytes=" + cumulative.length()
        + ", maxBufferedBytes=" + maxBufferedBytes);
      throw new IllegalStateException("SL427 receive buffer exceeded " + maxBufferedBytes + " bytes");
    }

    List<byte[]> frames = new ArrayList<>();
    while (true) {
      int start = indexOfStart(cumulative);
      if (start < 0) {
        if (cumulative.length() > 0) {
          log.debugRaw("stream", "未发现起始符，丢弃缓冲数据, discardedBytes=" + cumulative.length(),
            cumulative.getBytes());
        }
        cumulative = Buffer.buffer();
        break;
      }
      if (start > 0) {
        log.debugRaw("stream", "丢弃起始符前噪声, discardedBytes=" + start,
          cumulative.getBytes(0, start));
        cumulative = cumulative.getBuffer(start, cumulative.length());
      }
      if (cumulative.length() < 3) {
        log.debug("stream", "等待帧头剩余字节, bufferedBytes=" + cumulative.length());
        break;
      }

      int declaredLength = cumulative.getUnsignedByte(1);
      if (cumulative.getUnsignedByte(2) != START || declaredLength < MIN_USER_DATA_LENGTH) {
        log.debug("stream", "候选帧头无效, declaredLength=" + declaredLength
          + ", secondStart=0x" + Integer.toHexString(cumulative.getUnsignedByte(2)).toUpperCase());
        cumulative = cumulative.getBuffer(1, cumulative.length());
        continue;
      }

      int frameLength = declaredLength + 5;
      if (cumulative.length() < frameLength) {
        log.debug("stream", "等待完整帧, expectedBytes=" + frameLength
          + ", bufferedBytes=" + cumulative.length());
        break;
      }
      byte[] frame = cumulative.getBytes(0, frameLength);
      frames.add(frame);
      log.debugRaw("stream", "切出完整帧, frameBytes=" + frameLength, frame);
      cumulative = cumulative.getBuffer(frameLength, cumulative.length());
    }
    return frames;
  }

  public synchronized int bufferedBytes() {
    return cumulative.length();
  }

  public synchronized void reset() {
    log.debug("stream", "重置接收缓冲区, discardedBytes=" + cumulative.length());
    cumulative = Buffer.buffer();
  }

  private static int indexOfStart(Buffer buffer) {
    for (int i = 0; i < buffer.length(); i++) {
      if (buffer.getUnsignedByte(i) == START) {
        return i;
      }
    }
    return -1;
  }
}
