package io.github.kukpt.sl651.codec;

/**
 * @author shuo
 * 水文下行消息
 */
public class DownstreamMessage {

  public DownstreamMessage(MessageHeader messageHeader, DownstreamMessagePayload content, short frameControlType) {
    this.frameControlType = frameControlType;
    this.messageHeader = messageHeader;
    this.content = content;
  }

  public int functionType() {
    return messageHeader.functionType();
  }

  public MessageHeader messageHeader() {
    return messageHeader;
  }

  public DownstreamMessagePayload content() {
    return content;
  }

  public short frameControlType() {
    return frameControlType;
  }

  /**
   * 下行消息头
   */
  private final MessageHeader messageHeader;

  /**
   * 下行消息正文
   */
  private final DownstreamMessagePayload content;

  /**
   * 报文控制符号
   */
  private final short frameControlType;
}
