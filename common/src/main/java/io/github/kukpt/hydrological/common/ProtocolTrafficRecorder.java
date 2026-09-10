package io.github.kukpt.hydrological.common;

/** Sink for protocol-neutral raw traffic records. */
public interface ProtocolTrafficRecorder {
  ProtocolTrafficRecorder NOOP = record -> { };

  void record(ProtocolTrafficRecord record);
}
