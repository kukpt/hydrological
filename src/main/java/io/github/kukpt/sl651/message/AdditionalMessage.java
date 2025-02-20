package io.github.kukpt.sl651.message;

import io.github.kukpt.sl651.codec.ElementResult;
import io.github.kukpt.sl651.codec.PayloadDecode;
import io.netty.buffer.ByteBuf;

import java.util.Collection;

public class AdditionalMessage extends FixedBodyMessage {


  public Collection<ElementResult> elementResults() {
    return elementResults;
  }


  private final Collection<ElementResult> elementResults;

  public AdditionalMessage(ByteBuf buffer) {
    super(buffer);
    this.elementResults = PayloadDecode.decodeDefaultElementResults(buffer);
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("AdditionalMessage{");

    sb.append(", elementResults=").append(elementResults);
    sb.append('}');
    return sb.toString();
  }
}
