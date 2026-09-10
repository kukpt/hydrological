package io.github.kukpt.sl427.context;

import io.github.kukpt.sl427.frame.AddressMode;
import io.github.kukpt.sl427.model.ParameterType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class SimpleProtocolContext implements ProtocolContext {

  private final String protocolVersion;
  private final AddressMode addressMode;
  private final Map<ParameterType, List<ProtocolPoint>> points;
  private final Map<String, String> properties;

  private SimpleProtocolContext(Builder builder) {
    this.protocolVersion = builder.protocolVersion;
    this.addressMode = builder.addressMode;
    this.points = new EnumMap<>(ParameterType.class);
    for (Map.Entry<ParameterType, List<ProtocolPoint>> entry : builder.points.entrySet()) {
      this.points.put(entry.getKey(), Collections.unmodifiableList(new ArrayList<>(entry.getValue())));
    }
    this.properties = Collections.unmodifiableMap(new HashMap<>(builder.properties));
  }

  public static Builder builder() { return new Builder(); }

  @Override public String protocolVersion() { return protocolVersion; }
  @Override public AddressMode addressMode() { return addressMode; }

  @Override
  public List<? extends ProtocolPoint> points(ParameterType type) {
    List<ProtocolPoint> result = points.get(type);
    return result == null ? Collections.<ProtocolPoint>emptyList() : result;
  }

  @Override
  public Optional<String> property(String key) { return Optional.ofNullable(properties.get(key)); }

  public static final class Builder {
    private String protocolVersion = "2021";
    private AddressMode addressMode;
    private final Map<ParameterType, List<ProtocolPoint>> points = new EnumMap<>(ParameterType.class);
    private final Map<String, String> properties = new HashMap<>();

    public Builder protocolVersion(String protocolVersion) {
      this.protocolVersion = protocolVersion;
      return this;
    }

    public Builder addressMode(AddressMode addressMode) {
      this.addressMode = addressMode;
      return this;
    }

    public Builder addPoint(ProtocolPoint point) {
      List<ProtocolPoint> values = points.get(point.type());
      if (values == null) {
        values = new ArrayList<>();
        points.put(point.type(), values);
      }
      values.add(point);
      return this;
    }

    public Builder property(String key, String value) {
      properties.put(key, value);
      return this;
    }

    public SimpleProtocolContext build() { return new SimpleProtocolContext(this); }
  }
}
