package io.github.kukpt.sl427.context;

import io.github.kukpt.sl427.frame.StationAddress;

/** Resolves protocol configuration for an SL427 station address. */
@FunctionalInterface
public interface ProtocolContextResolver {

  ProtocolContext resolve(StationAddress address);
}
