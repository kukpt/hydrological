package io.github.kukpt.sl427.schema;

import io.github.kukpt.sl427.frame.Direction;

import java.util.Objects;

public final class SchemaKey {

  public static final int ANY_TYPE = -1;

  private final Direction direction;
  private final int afn;
  private final int typeCode;
  private final MessageRole role;

  public SchemaKey(Direction direction, int afn, int typeCode, MessageRole role) {
    this.direction = direction;
    this.afn = afn;
    this.typeCode = typeCode;
    this.role = role;
  }

  public Direction direction() { return direction; }
  public int afn() { return afn; }
  public int typeCode() { return typeCode; }
  public MessageRole role() { return role; }

  @Override
  public boolean equals(Object other) {
    if (!(other instanceof SchemaKey)) return false;
    SchemaKey that = (SchemaKey) other;
    return afn == that.afn && typeCode == that.typeCode && direction == that.direction && role == that.role;
  }

  @Override
  public int hashCode() { return Objects.hash(direction, afn, typeCode, role); }
}
