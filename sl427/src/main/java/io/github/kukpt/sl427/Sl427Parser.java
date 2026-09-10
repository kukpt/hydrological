package io.github.kukpt.sl427;

import io.github.kukpt.hydrological.common.ProtocolDecodeLogger;
import io.github.kukpt.hydrological.common.ProtocolTrafficRecord;
import io.github.kukpt.hydrological.common.ProtocolTrafficRecorder;
import io.github.kukpt.hydrological.common.ProtocolType;
import io.github.kukpt.hydrological.common.TrafficDirection;
import io.github.kukpt.sl427.assembly.Sl427FrameAssembler;
import io.github.kukpt.sl427.codec.Sl427FrameParser;
import io.github.kukpt.sl427.codec.Sl427StreamDecoder;
import io.github.kukpt.sl427.context.EmptyProtocolContext;
import io.github.kukpt.sl427.context.ProtocolContext;
import io.github.kukpt.sl427.context.ProtocolContextResolver;
import io.github.kukpt.sl427.frame.Sl427Frame;
import io.github.kukpt.sl427.frame.AddressMode;
import io.github.kukpt.sl427.frame.StationAddress;
import io.github.kukpt.sl427.model.CommandMessage;
import io.github.kukpt.sl427.model.LinkDetectionMessage;
import io.github.kukpt.sl427.model.LinkDetectionType;
import io.github.kukpt.sl427.model.ProtocolMessage;
import io.github.kukpt.sl427.model.RawAfnMessage;
import io.github.kukpt.sl427.parser.RealtimeDataParser;
import io.github.kukpt.sl427.result.ParseIssue;
import io.github.kukpt.sl427.result.ParseResult;
import io.github.kukpt.sl427.result.ParseStatus;
import io.github.kukpt.sl427.schema.BodyParts;
import io.github.kukpt.sl427.schema.MessageRole;
import io.github.kukpt.sl427.schema.MessageSchema;
import io.github.kukpt.sl427.schema.MessageSchemaRegistry;
import io.vertx.core.buffer.Buffer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/** Stateful, per-connection SL/T 427-2021 stream and message parser. */
public final class Sl427Parser {
  private static final ProtocolDecodeLogger log =
    ProtocolDecodeLogger.getLogger(Sl427Parser.class, ProtocolType.SL427);

  private final Sl427StreamDecoder streamDecoder;
  private final Sl427FrameParser frameParser;
  private final Sl427FrameAssembler assembler;
  private final MessageSchemaRegistry schemas;
  private final RealtimeDataParser realtimeParser;
  private final ProtocolTrafficRecorder trafficRecorder;
  private final String defaultEndpointId;
  private final Function<StationAddress, String> endpointIdResolver;
  private final String remoteAddress;
  private final boolean includeParsedData;

  public Sl427Parser() {
    this((String) null, null, ProtocolTrafficRecorder.NOOP);
  }

  /** Creates a parser which records every complete inbound SL427 frame. */
  public Sl427Parser(String endpointId, String remoteAddress, ProtocolTrafficRecorder trafficRecorder) {
    this(address -> endpointId, endpointId, remoteAddress, trafficRecorder);
  }

  /** Creates a parser whose traffic endpoint id can be derived from a decoded station address. */
  public Sl427Parser(Function<StationAddress, String> endpointIdResolver, String defaultEndpointId,
      String remoteAddress, ProtocolTrafficRecorder trafficRecorder) {
    this(endpointIdResolver, defaultEndpointId, remoteAddress, trafficRecorder,
      AddressMode.ADMINISTRATIVE);
  }

  /** Creates a parser using the configured station-address representation. */
  public Sl427Parser(Function<StationAddress, String> endpointIdResolver, String defaultEndpointId,
      String remoteAddress, ProtocolTrafficRecorder trafficRecorder, AddressMode addressMode) {
    this(endpointIdResolver, defaultEndpointId, remoteAddress, trafficRecorder, addressMode, true);
  }

  /** Creates a parser with optional structured traffic snapshots. */
  public Sl427Parser(Function<StationAddress, String> endpointIdResolver, String defaultEndpointId,
      String remoteAddress, ProtocolTrafficRecorder trafficRecorder, AddressMode addressMode,
      boolean includeParsedData) {
    this.streamDecoder = new Sl427StreamDecoder();
    this.frameParser = new Sl427FrameParser(addressMode);
    this.assembler = new Sl427FrameAssembler();
    this.schemas = new MessageSchemaRegistry();
    this.realtimeParser = new RealtimeDataParser();
    this.endpointIdResolver = endpointIdResolver;
    this.defaultEndpointId = defaultEndpointId;
    this.remoteAddress = remoteAddress;
    this.trafficRecorder = trafficRecorder == null ? ProtocolTrafficRecorder.NOOP : trafficRecorder;
    this.includeParsedData = includeParsedData;
  }

  /** Feeds an arbitrary TCP chunk; zero, one, or many complete results may be returned. */
  public List<ParseResult<ProtocolMessage>> accept(Buffer incoming, ProtocolContext context) {
    return accept(incoming, null, context);
  }

  /** Resolves context after the station address has been decoded, with a fixed fallback context. */
  public List<ParseResult<ProtocolMessage>> accept(Buffer incoming,
      ProtocolContextResolver contextResolver, ProtocolContext fallbackContext) {
    List<ParseResult<ProtocolMessage>> results = new ArrayList<>();
    for (byte[] raw : streamDecoder.decode(incoming)) {
      results.add(parseInternal(raw, fallbackContext, contextResolver));
    }
    return results;
  }

  public List<ParseResult<ProtocolMessage>> accept(Buffer incoming) {
    return accept(incoming, EmptyProtocolContext.INSTANCE);
  }

  public ParseResult<ProtocolMessage> parse(byte[] raw) {
    return parse(raw, EmptyProtocolContext.INSTANCE);
  }

  public ParseResult<ProtocolMessage> parse(byte[] raw, ProtocolContext context) {
    return parseInternal(raw, context, null);
  }

  public ParseResult<ProtocolMessage> parse(byte[] raw,
      ProtocolContextResolver contextResolver, ProtocolContext fallbackContext) {
    return parseInternal(raw, fallbackContext, contextResolver);
  }

  private ParseResult<ProtocolMessage> parseInternal(byte[] raw, ProtocolContext context,
      ProtocolContextResolver contextResolver) {
    log.debugRaw("message", "开始业务解码", raw);
    ParseResult<Sl427Frame> parsed = frameParser.parse(raw);
    if (!parsed.isSuccess()) {
      return finish("frame", copyFailure(parsed), raw, null);
    }
    Sl427Frame frame = parsed.value();
    StationAddress sourceAddress = frame.address();

    ProtocolContext effectiveContext = context;
    if (contextResolver != null) {
      effectiveContext = contextResolver.resolve(frame.address());
    }
    if (effectiveContext == null) effectiveContext = EmptyProtocolContext.INSTANCE;

    if (effectiveContext.addressMode() != null && effectiveContext.addressMode() != frame.address().mode()) {
      return finish("context", ParseResult.failure(ParseStatus.CONFIG_MISMATCH,
        new ParseIssue("ADDRESS_MODE_MISMATCH", "context.addressMode", -1,
          effectiveContext.addressMode().name(), frame.address().mode().name(),
          "configured address mode does not match the frame"), raw), raw, sourceAddress);
    }

    if (isImage(frame)) {
      return finish("dispatch", ParseResult.of(ParseStatus.UNSUPPORTED, new RawAfnMessage(frame),
        Collections.singletonList(ParseIssue.of("UNSUPPORTED_IMAGE_FRAME", "afn", 9,
          "SL427 image reports are intentionally not parsed")), raw), raw, sourceAddress);
    }

    ParseResult<Sl427Frame> assembled = assembler.accept(frame);
    if (assembled.status() == ParseStatus.PARTIAL) {
      return finish("assembly", ParseResult.of(ParseStatus.PARTIAL, null,
        assembled.issues(), assembled.raw()), raw, sourceAddress);
    }
    if (!assembled.isSuccess()) {
      return finish("assembly", copyFailure(assembled), raw, sourceAddress);
    }
    frame = assembled.value();

    Optional<MessageSchema> schema = schemas.find(frame);
    if (!schema.isPresent()) {
      return finish("schema", ParseResult.of(ParseStatus.UNSUPPORTED, new RawAfnMessage(frame),
        Collections.singletonList(ParseIssue.of("UNSUPPORTED_AFN", "afn", -1,
          "AFN has no registered business schema")), frame.rawFrame()), raw, sourceAddress);
    }
    log.debug("schema", "匹配消息结构, afn=" + frame.afn() + ", role=" + schema.get().key().role()
      + ", aux=" + schema.get().auxLayout());
    ParseResult<BodyParts> split = schema.get().split(frame);
    if (!split.isSuccess()) return finish("body", copyFailure(split), raw, sourceAddress);
    BodyParts parts = split.value();
    log.debug("body", "正文拆分完成, dataBytes=" + parts.data().length
      + ", password=" + (parts.password() != null) + ", timeTag=" + (parts.timeTag() != null));

    if (frame.afn().value() == 0x02) {
      return finish("link", parseLink(frame, parts), raw, sourceAddress);
    }
    MessageRole role = schema.get().key().role();
    if ((frame.afn().value() == 0xB0 && role == MessageRole.RESPONSE) ||
        (frame.afn().value() == 0xC0 && role == MessageRole.REPORT) ||
        (frame.afn().value() == 0x84 && role == MessageRole.REPORT)) {
      return finish("realtime", realtimeParser.parse(frame, parts, effectiveContext),
        raw, sourceAddress);
    }
    return finish("command", ParseResult.<ProtocolMessage>success(
      new CommandMessage(frame, role, parts.data(), parts.timeTag()), frame.rawFrame()),
      raw, sourceAddress);
  }

  public int bufferedBytes() { return streamDecoder.bufferedBytes(); }
  public int pendingAssemblies() { return assembler.pendingCount(); }
  public void resetStream() { streamDecoder.reset(); }

  private void record(byte[] raw, StationAddress address, ParseResult<ProtocolMessage> result) {
    if (raw == null) return;
    try {
      String endpointId = defaultEndpointId;
      if (address != null && endpointIdResolver != null) {
        String resolved = endpointIdResolver.apply(address);
        if (resolved != null) endpointId = resolved;
      }
      io.vertx.core.json.JsonObject parsedData = null;
      String parseError = issueSummary(result.issues());
      if (includeParsedData && result.value() != null) {
        try {
          parsedData = Sl427ParsedDataJson.encode(result.value());
        } catch (RuntimeException error) {
          parseError = appendError(parseError, "snapshot: " + safeMessage(error));
        }
      }
      trafficRecorder.record(new ProtocolTrafficRecord(ProtocolType.SL427,
        TrafficDirection.INBOUND, endpointId, remoteAddress,
        ProtocolDecodeLogger.fullHex(raw), result.status().name(), parsedData, parseError));
    } catch (RuntimeException error) {
      log.error("record", "原始协议报文记录失败", error);
    }
  }

  private ParseResult<ProtocolMessage> finish(String stage, ParseResult<ProtocolMessage> result,
      byte[] raw, StationAddress address) {
    record(raw, address, result);
    return logged(stage, result);
  }

  private static String issueSummary(List<ParseIssue> issues) {
    if (issues == null || issues.isEmpty()) return null;
    StringBuilder summary = new StringBuilder();
    for (ParseIssue issue : issues) {
      if (summary.length() > 0) summary.append("; ");
      summary.append(issue.code()).append(": ").append(issue.message());
      if (summary.length() >= 512) return summary.substring(0, 512);
    }
    return summary.toString();
  }

  private static String appendError(String current, String addition) {
    return current == null ? addition : current + "; " + addition;
  }

  private static String safeMessage(Throwable error) {
    String message = error.getMessage();
    if (message == null) message = error.getClass().getSimpleName();
    return message.length() > 256 ? message.substring(0, 256) : message;
  }

  private static ParseResult<ProtocolMessage> logged(String stage, ParseResult<ProtocolMessage> result) {
    String detail = "status=" + result.status();
    if (!result.issues().isEmpty()) detail += ", issues=" + result.issues();
    if (result.isSuccess() || result.status() == ParseStatus.PARTIAL) {
      log.debug(stage, "解码阶段完成, " + detail);
    } else {
      log.warn(stage, "解码阶段结束, " + detail);
    }
    return result;
  }

  private static ParseResult<ProtocolMessage> parseLink(Sl427Frame frame, BodyParts parts) {
    byte[] data = parts.data();
    if (data.length != 1) {
      return ParseResult.failure(ParseStatus.INVALID_DATA,
        new ParseIssue("LINK_DATA_LENGTH", "data", 0, "1", Integer.toString(data.length),
          "link detection data must contain exactly one byte"), frame.rawFrame());
    }
    try {
      return ParseResult.<ProtocolMessage>success(
        new LinkDetectionMessage(frame, LinkDetectionType.from(data[0] & 0xFF)), frame.rawFrame());
    } catch (IllegalArgumentException error) {
      return ParseResult.failure(ParseStatus.INVALID_DATA,
        ParseIssue.of("UNKNOWN_LINK_TYPE", "data", 0, error.getMessage()), frame.rawFrame());
    }
  }

  private static boolean isImage(Sl427Frame frame) {
    return frame.afn().value() == 0x61 || frame.afn().value() == 0x83;
  }

  private static <T> ParseResult<ProtocolMessage> copyFailure(ParseResult<T> source) {
    return ParseResult.of(source.status(), null, source.issues(), source.raw());
  }
}
