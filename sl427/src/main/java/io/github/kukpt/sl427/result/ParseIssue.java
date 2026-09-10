package io.github.kukpt.sl427.result;

import java.util.Objects;

public final class ParseIssue {

  private final String code;
  private final String field;
  private final int offset;
  private final String expected;
  private final String actual;
  private final String message;

  public ParseIssue(String code, String field, int offset, String expected, String actual, String message) {
    this.code = Objects.requireNonNull(code, "code");
    this.field = field;
    this.offset = offset;
    this.expected = expected;
    this.actual = actual;
    this.message = Objects.requireNonNull(message, "message");
  }

  public static ParseIssue of(String code, String field, int offset, String message) {
    return new ParseIssue(code, field, offset, null, null, message);
  }

  public String code() { return code; }
  public String field() { return field; }
  public int offset() { return offset; }
  public String expected() { return expected; }
  public String actual() { return actual; }
  public String message() { return message; }

  @Override
  public String toString() {
    return "ParseIssue{" +
      "code='" + code + '\'' +
      ", field='" + field + '\'' +
      ", offset=" + offset +
      ", expected='" + expected + '\'' +
      ", actual='" + actual + '\'' +
      ", message='" + message + '\'' +
      '}';
  }
}
