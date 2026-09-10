package io.github.kukpt.sl651.message;

import io.github.kukpt.sl651.codec.element.ElementResult;

import java.util.Collection;

/** Message body containing decoded SL651 observation elements. */
public interface ElementMessage extends IMessageBody {
  Collection<ElementResult> elementResults();
}
