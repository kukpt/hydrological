package io.github.kukpt.sl651.codec;

/**
 * Payloads that carry a protocol stream id.
 */
public interface StreamIdentifiedPayload {

  int streamId();
}
