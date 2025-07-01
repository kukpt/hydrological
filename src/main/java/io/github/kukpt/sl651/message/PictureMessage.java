package io.github.kukpt.sl651.message;

import io.github.kukpt.sl651.codec.element.ElementDecodeUtils;
import io.github.kukpt.sl651.codec.element.ElementId;
import io.netty.buffer.ByteBuf;
import io.vertx.core.buffer.Buffer;

public class PictureMessage extends FixedBodyMessage {

  public PictureMessage(ByteBuf byteBuf) {
    super(byteBuf);
    Buffer picBuf =  Buffer.buffer();
    ElementId elementId = ElementDecodeUtils.decodeElementId(byteBuf);
    if (0xF3 == elementId.id()) {
      byte[] bytes = new byte[byteBuf.readableBytes()];
      byteBuf.readBytes(bytes);
      picBuf = Buffer.buffer(bytes);
    }
    picture = picBuf;
  }

  private final Buffer picture;

  public Buffer getPicture() {
    return picture;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("PictureMessage{");
    sb.append(super.toString());
    sb.append('}');
    return sb.toString();
  }
}
