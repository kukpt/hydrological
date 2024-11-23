package io.github.kukpt.sl651.codec;

import java.util.Collection;

public class AdditionalMessage {

  public FixedBodyMessage fixedBodyMessage() {
    return fixedBodyMessage;
  }

  public Collection<ElementResult> elementResults() {
    return elementResults;
  }

  private final FixedBodyMessage fixedBodyMessage;

  private final Collection<ElementResult> elementResults;

  AdditionalMessage(FixedBodyMessage fixedBodyMessage, Collection<ElementResult> elementResults) {
    this.fixedBodyMessage = fixedBodyMessage;
    this.elementResults = elementResults;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("AdditionalMessage{");
    sb.append("fixedBodyMessage=").append(fixedBodyMessage);
    sb.append(", elementResults=").append(elementResults);
    sb.append('}');
    return sb.toString();
  }
}
