package io.github.kukpt.sl427.schema;

import io.github.kukpt.sl427.frame.Sl427Frame;
import io.github.kukpt.sl427.frame.TimeTag;
import io.github.kukpt.sl427.result.ParseIssue;
import io.github.kukpt.sl427.result.ParseResult;
import io.github.kukpt.sl427.result.ParseStatus;

import java.util.Arrays;

public final class MessageSchema {

  private final SchemaKey key;
  private final AuxLayout auxLayout;

  public MessageSchema(SchemaKey key, AuxLayout auxLayout) {
    this.key = key;
    this.auxLayout = auxLayout;
  }

  public SchemaKey key() { return key; }
  public AuxLayout auxLayout() { return auxLayout; }

  public ParseResult<BodyParts> split(Sl427Frame frame) {
    byte[] body = frame.bodyRaw();
    if (body.length < auxLayout.length()) {
      return ParseResult.failure(ParseStatus.INVALID_DATA,
        new ParseIssue("AUX_TRUNCATED", "body", 0,
          Integer.toString(auxLayout.length()), Integer.toString(body.length), "body is shorter than required AUX"),
        frame.rawFrame());
    }
    int dataEnd = body.length - auxLayout.length();
    byte[] password = null;
    TimeTag timeTag = null;
    try {
      int cursor = dataEnd;
      if (auxLayout.hasPassword()) {
        password = Arrays.copyOfRange(body, cursor, cursor + 2);
        cursor += 2;
      }
      if (auxLayout.hasTimeTag()) {
        timeTag = TimeTag.parse(body, cursor);
      }
      return ParseResult.success(
        new BodyParts(Arrays.copyOfRange(body, 0, dataEnd), password, timeTag), frame.rawFrame());
    } catch (IllegalArgumentException error) {
      return ParseResult.failure(ParseStatus.INVALID_DATA,
        ParseIssue.of("INVALID_AUX", "aux", dataEnd, error.getMessage()), frame.rawFrame());
    }
  }
}
