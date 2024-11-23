package io.github.kukpt.sl651.codec;


import java.util.Collection;

public class PeriodMessage {

  private final FixedBodyMessage fixedBodyMessage;

  private final Collection<ElementResult> elementResults;
  /**
   * 时间步长
   */
  private final TimeStep timeStep;

  private final ElementId elementId;

  PeriodMessage(FixedBodyMessage fixedBodyMessage, TimeStep timeStep, ElementId elementId, Collection<ElementResult> elementResults) {
    this.fixedBodyMessage = fixedBodyMessage;
    this.elementResults = elementResults;
    this.timeStep = timeStep;
    this.elementId = elementId;
  }

  public FixedBodyMessage fixedBodyMessage() {
    return fixedBodyMessage;
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
    sb.append("fixedBodyMessage=").append(fixedBodyMessage);
    sb.append(", elementResults=").append(elementResults);
    sb.append(", timeStep=").append(timeStep);
    sb.append(", elementId=").append(elementId);
    sb.append('}');
    return sb.toString();
  }
}
