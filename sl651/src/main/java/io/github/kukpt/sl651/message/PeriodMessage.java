package io.github.kukpt.sl651.message;

import io.github.kukpt.sl651.codec.*;
import io.github.kukpt.sl651.codec.element.ElementDecodeUtils;
import io.github.kukpt.sl651.codec.element.ElementId;
import io.github.kukpt.sl651.codec.element.ElementResult;
import io.netty.buffer.ByteBuf;

import java.util.ArrayList;
import java.util.Collection;

public class PeriodMessage extends FixedBodyMessage {


  private final Collection<ElementResult> elementResults = new ArrayList<>();
  /**
   * 时间步长
   */
  private final TimeStep timeStep;

  private final ElementId elementId;

  public PeriodMessage(ByteBuf byteBuf) {
    super(byteBuf);
    this.timeStep = TimeStep.createTimeStep(byteBuf);
    this.elementId = ElementDecodeUtils.decodeElementId(byteBuf);
    while (byteBuf.isReadable()) {
      ElementResult elementResult = ElementDecodeUtils.decodeElement(byteBuf, elementId);
      elementResults.add(elementResult);
    }
  }


  public Collection<ElementResult> elementResults() {
    return elementResults;
  }

  public TimeStep timeStep() {
    return timeStep;
  }

  public ElementId elementId() {
    return elementId;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("PeriodMessage{");
    sb.append(super.toString());
    sb.append(", elementResults=").append(elementResults);
    sb.append(", timeStep=").append(timeStep);
    sb.append(", elementId=").append(elementId);
    sb.append('}');
    return sb.toString();
  }
}
