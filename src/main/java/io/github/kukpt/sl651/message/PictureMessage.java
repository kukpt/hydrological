package io.github.kukpt.sl651.message;

import io.netty.buffer.ByteBuf;
import io.vertx.core.buffer.Buffer;

public class PictureMessage extends FixedBodyMessage {

  public PictureMessage(ByteBuf byteBuf) {
    super(byteBuf);
    Buffer picBuf =  Buffer.buffer();
    int pictureElement = byteBuf.readUnsignedShort();
    if (0xF3F3 == pictureElement) {
      picBuf = Buffer.buffer(byteBuf);
    }
    picture = picBuf;
  }

  private final Buffer picture;

  public Buffer getPicture() {
    return picture;
  }

}
