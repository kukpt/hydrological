package io.github.kukpt.sl651.message;

import io.github.kukpt.sl651.codec.element.ElementDecodeUtils;
import io.github.kukpt.sl651.codec.element.ElementResult;
import io.netty.buffer.ByteBuf;

import java.util.Collection;

public class TestMessage extends FixedBodyMessage{

  private final Collection<ElementResult> elementResults;

  public TestMessage(ByteBuf buffer) {
    super(buffer);
    this.elementResults = ElementDecodeUtils.decodeDefaultElementResults(buffer);
  }

  public Collection<ElementResult> elementResults() {
    return elementResults;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("TestMessage{");
    sb.append(", elementResults=").append(elementResults);
    sb.append('}');
    return sb.toString();
  }
}
