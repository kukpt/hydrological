package io.github.kukpt.sl427.model;

import io.github.kukpt.sl427.frame.Sl427Frame;
import io.github.kukpt.sl427.frame.TimeTag;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class RealtimeDataMessage implements ProtocolMessage {

  private final Sl427Frame frame;
  private final ParameterType parameterType;
  private final List<Measurement> measurements;
  private final StatusField status;
  private final TimeTag timeTag;

  public RealtimeDataMessage(Sl427Frame frame, ParameterType parameterType,
                             List<Measurement> measurements, StatusField status, TimeTag timeTag) {
    this.frame = frame;
    this.parameterType = parameterType;
    this.measurements = Collections.unmodifiableList(new ArrayList<>(measurements));
    this.status = status;
    this.timeTag = timeTag;
  }

  @Override
  public Sl427Frame frame() { return frame; }
  public ParameterType parameterType() { return parameterType; }
  public List<Measurement> measurements() { return measurements; }
  public StatusField status() { return status; }
  public TimeTag timeTag() { return timeTag; }
}
