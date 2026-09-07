package io.github.kukpt.sl651.message;

import io.github.kukpt.sl651.codec.element.ElementDecodeUtils;
import io.github.kukpt.sl651.codec.element.ElementId;
import io.github.kukpt.sl651.codec.element.ElementResult;
import io.netty.buffer.ByteBuf;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class TimingMessage extends FixedBodyMessage {

  private final Collection<ElementResult> elementResults;

  public TimingMessage(ByteBuf buffer) {
    super(buffer);
    this.elementResults = ElementDecodeUtils.decodeDefaultElementResults(buffer);
  }

  public Collection<ElementResult> elementResults() {
    return elementResults;
  }

  public Map<Integer, ElementResult> resultMap() {
    Map<Integer, ElementResult> resultMap = new HashMap<>();
    for (ElementResult result : elementResults) {
      resultMap.put(result.elementId().id(), result);
    }
    return resultMap;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("TimingMessage{");
    sb.append(super.toString());
    sb.append(", elementResults=").append(elementResults);
    sb.append('}');
    return sb.toString();
  }
}
