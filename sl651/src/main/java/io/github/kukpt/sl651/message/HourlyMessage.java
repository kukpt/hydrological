package io.github.kukpt.sl651.message;

import io.github.kukpt.sl651.codec.element.ElementDecodeUtils;
import io.github.kukpt.sl651.codec.element.ElementResult;
import io.netty.buffer.ByteBuf;

import java.util.Collection;

public class HourlyMessage extends FixedBodyMessage{

  private final Collection<ElementResult> elementResults;

  public Collection<ElementResult> elementResults() {
    return elementResults;
  }

  public HourlyMessage(ByteBuf buffer) {
    super(buffer);
    this.elementResults = ElementDecodeUtils.decodeDefaultElementResults(buffer);
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("HourlyMessage{");
    sb.append(super.toString());
    sb.append(", elementResults=").append(elementResults);
    sb.append('}');
    return sb.toString();
  }
}
