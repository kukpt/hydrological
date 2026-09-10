package io.github.kukpt.sl427.result;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ParseResult<T> {

  private final ParseStatus status;
  private final T value;
  private final List<ParseIssue> issues;
  private final byte[] raw;

  private ParseResult(ParseStatus status, T value, List<ParseIssue> issues, byte[] raw) {
    this.status = status;
    this.value = value;
    this.issues = Collections.unmodifiableList(new ArrayList<>(issues));
    this.raw = raw == null ? new byte[0] : raw.clone();
  }

  public static <T> ParseResult<T> success(T value, byte[] raw) {
    return new ParseResult<>(ParseStatus.SUCCESS, value, Collections.<ParseIssue>emptyList(), raw);
  }

  public static <T> ParseResult<T> of(ParseStatus status, T value, List<ParseIssue> issues, byte[] raw) {
    return new ParseResult<>(status, value,
      issues == null ? Collections.<ParseIssue>emptyList() : issues, raw);
  }

  public static <T> ParseResult<T> failure(ParseStatus status, ParseIssue issue, byte[] raw) {
    return new ParseResult<>(status, null, Collections.singletonList(issue), raw);
  }

  public ParseStatus status() { return status; }
  public T value() { return value; }
  public List<ParseIssue> issues() { return issues; }
  public byte[] raw() { return raw.clone(); }
  public boolean isSuccess() { return status == ParseStatus.SUCCESS; }
}
