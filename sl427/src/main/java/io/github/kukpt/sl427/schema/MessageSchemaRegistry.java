package io.github.kukpt.sl427.schema;

import io.github.kukpt.sl427.frame.Direction;
import io.github.kukpt.sl427.frame.Sl427Frame;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class MessageSchemaRegistry {

  private final Map<SchemaKey, MessageSchema> schemas;

  public MessageSchemaRegistry() {
    Map<SchemaKey, MessageSchema> values = new HashMap<>();
    register(values, Direction.DOWNSTREAM, 0x02, SchemaKey.ANY_TYPE, MessageRole.LINK, AuxLayout.NONE);
    register(values, Direction.UPSTREAM, 0x02, SchemaKey.ANY_TYPE, MessageRole.LINK, AuxLayout.NONE);
    for (int type = 1; type <= 15; type++) {
      register(values, Direction.DOWNSTREAM, 0xB0, type, MessageRole.QUERY, AuxLayout.NONE);
      register(values, Direction.UPSTREAM, 0xB0, type, MessageRole.RESPONSE, AuxLayout.NONE);
      register(values, Direction.UPSTREAM, 0x81, type, MessageRole.REPORT, AuxLayout.TIME_TAG_7);
      register(values, Direction.UPSTREAM, 0x82, type, MessageRole.REPORT, AuxLayout.TIME_TAG_7);
      register(values, Direction.UPSTREAM, 0xC0, type, MessageRole.REPORT, AuxLayout.TIME_TAG_7);
    }
    register(values, Direction.DOWNSTREAM, 0x81, 0, MessageRole.CONFIRM, AuxLayout.TIME_TAG_7);
    register(values, Direction.DOWNSTREAM, 0x82, 0, MessageRole.CONFIRM, AuxLayout.TIME_TAG_7);
    register(values, Direction.DOWNSTREAM, 0xC0, 0, MessageRole.CONFIRM, AuxLayout.TIME_TAG_7);
    register(values, Direction.UPSTREAM, 0x84, 13, MessageRole.REPORT, AuxLayout.NONE);
    register(values, Direction.DOWNSTREAM, 0x84, 0, MessageRole.CONFIRM, AuxLayout.TIME_TAG_7);
    schemas = Collections.unmodifiableMap(values);
  }

  public Optional<MessageSchema> find(Sl427Frame frame) {
    MessageRole role = roleOf(frame);
    SchemaKey exact = new SchemaKey(frame.control().direction(), frame.afn().value(), frame.control().typeCode(), role);
    MessageSchema schema = schemas.get(exact);
    if (schema == null) {
      schema = schemas.get(new SchemaKey(frame.control().direction(), frame.afn().value(), SchemaKey.ANY_TYPE, role));
    }
    return Optional.ofNullable(schema);
  }

  public MessageRole roleOf(Sl427Frame frame) {
    int afn = frame.afn().value();
    Direction direction = frame.control().direction();
    if (afn == 0x02) return MessageRole.LINK;
    if (afn == 0xB0) return direction == Direction.DOWNSTREAM ? MessageRole.QUERY : MessageRole.RESPONSE;
    if (afn == 0x81 || afn == 0x82 || afn == 0x83 || afn == 0x84 || afn == 0xC0) {
      return direction == Direction.UPSTREAM ? MessageRole.REPORT : MessageRole.CONFIRM;
    }
    if (direction == Direction.UPSTREAM) return frame.control().typeCode() == 0 ? MessageRole.CONFIRM : MessageRole.RESPONSE;
    return frame.control().typeCode() == 0 ? MessageRole.COMMAND : MessageRole.QUERY;
  }

  private static void register(Map<SchemaKey, MessageSchema> values, Direction direction, int afn,
                               int type, MessageRole role, AuxLayout aux) {
    SchemaKey key = new SchemaKey(direction, afn, type, role);
    if (values.put(key, new MessageSchema(key, aux)) != null) {
      throw new IllegalStateException("duplicate SL427 schema");
    }
  }
}
