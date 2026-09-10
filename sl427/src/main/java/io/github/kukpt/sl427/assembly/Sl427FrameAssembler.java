package io.github.kukpt.sl427.assembly;

import io.github.kukpt.hydrological.common.ProtocolDecodeLogger;
import io.github.kukpt.hydrological.common.ProtocolType;
import io.github.kukpt.sl427.frame.Sl427Frame;
import io.github.kukpt.sl427.result.ParseIssue;
import io.github.kukpt.sl427.result.ParseResult;
import io.github.kukpt.sl427.result.ParseStatus;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/** Per-connection assembler for ordinary split frames. Image AFNs are intentionally excluded by Sl427Parser. */
public final class Sl427FrameAssembler {

  private static final ProtocolDecodeLogger log =
    ProtocolDecodeLogger.getLogger(Sl427FrameAssembler.class, ProtocolType.SL427);

  private final int maxConcurrent;
  private final int maxAssembledBytes;
  private final long timeoutMillis;
  private final Map<String, State> states = new HashMap<>();

  public Sl427FrameAssembler() {
    this(128, 4 * 1024 * 1024, 120_000L);
  }

  public Sl427FrameAssembler(int maxConcurrent, int maxAssembledBytes, long timeoutMillis) {
    this.maxConcurrent = maxConcurrent;
    this.maxAssembledBytes = maxAssembledBytes;
    this.timeoutMillis = timeoutMillis;
  }

  public synchronized ParseResult<Sl427Frame> accept(Sl427Frame frame) {
    return accept(frame, System.currentTimeMillis());
  }

  public synchronized ParseResult<Sl427Frame> accept(Sl427Frame frame, long nowMillis) {
    evictExpired(nowMillis);
    if (!frame.control().split()) {
      log.debug("assembly", "单帧报文无需组包, address=" + frame.address() + ", afn=" + frame.afn());
      return ParseResult.success(frame, frame.rawFrame());
    }
    int divs = frame.splitCountdown();
    String key = key(frame);
    State state = states.get(key);
    if (state == null) {
      if (states.size() >= maxConcurrent) {
        return failure("ASSEMBLY_LIMIT", "too many concurrent split messages", frame);
      }
      state = new State(frame, divs, nowMillis + timeoutMillis);
      states.put(key, state);
      log.debug("assembly", "创建分帧组包状态, key=" + key + ", divs=" + divs
        + ", fragmentBytes=" + frame.bodyRaw().length);
    } else {
      if (divs == state.previousDivs) {
        if (Arrays.equals(frame.bodyRaw(), state.fragments.get(state.fragments.size() - 1))) {
          return ParseResult.of(ParseStatus.PARTIAL, null,
            Arrays.asList(ParseIssue.of("DUPLICATE_FRAGMENT", "control.divs", 4, "duplicate fragment ignored")),
            frame.rawFrame());
        }
        states.remove(key);
        return failure("FRAGMENT_CONFLICT", "same DIVS contains different data", frame);
      }
      if (divs != state.previousDivs - 1) {
        states.remove(key);
        return failure("DIVS_SEQUENCE", "split countdown is not continuous", frame);
      }
      state.add(frame, divs, nowMillis + timeoutMillis);
      log.debug("assembly", "追加分帧, key=" + key + ", divs=" + divs
        + ", fragments=" + state.fragments.size() + ", totalBytes=" + state.totalBytes);
    }

    if (state.totalBytes > maxAssembledBytes) {
      states.remove(key);
      return failure("ASSEMBLED_SIZE_LIMIT", "assembled body exceeds configured limit", frame);
    }
    if (divs != 1) {
      log.debug("assembly", "等待后续分帧, key=" + key + ", nextDivs=" + (divs - 1));
      return ParseResult.of(ParseStatus.PARTIAL, null, new ArrayList<ParseIssue>(), frame.rawFrame());
    }

    states.remove(key);
    byte[] body = concatenate(state.fragments);
    byte[] sourceFrames = concatenate(state.sourceFrames);
    log.debug("assembly", "分帧组包完成, key=" + key + ", fragments=" + state.fragments.size()
      + ", bodyBytes=" + body.length);
    return ParseResult.success(state.first.assembled(body, sourceFrames), sourceFrames);
  }

  public synchronized int pendingCount() { return states.size(); }

  private void evictExpired(long nowMillis) {
    Iterator<State> iterator = states.values().iterator();
    int evicted = 0;
    while (iterator.hasNext()) {
      if (iterator.next().deadlineMillis <= nowMillis) {
        iterator.remove();
        evicted++;
      }
    }
    if (evicted > 0) log.warn("assembly", "清理超时组包状态, count=" + evicted);
  }

  private static String key(Sl427Frame frame) {
    return frame.control().direction() + ":" + frame.address().canonicalKey() + ":" +
      frame.afn().value() + ":" + frame.control().typeCode();
  }

  private static ParseResult<Sl427Frame> failure(String code, String message, Sl427Frame frame) {
    log.warn("assembly", "组包失败, code=" + code + ", message=" + message
      + ", address=" + frame.address() + ", afn=" + frame.afn());
    return ParseResult.failure(ParseStatus.INVALID_DATA,
      ParseIssue.of(code, "assembly", -1, message), frame.rawFrame());
  }

  private static byte[] concatenate(List<byte[]> parts) {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    for (byte[] part : parts) output.write(part, 0, part.length);
    return output.toByteArray();
  }

  private static final class State {
    private final Sl427Frame first;
    private final List<byte[]> fragments = new ArrayList<>();
    private final List<byte[]> sourceFrames = new ArrayList<>();
    private int previousDivs;
    private int totalBytes;
    private long deadlineMillis;

    private State(Sl427Frame frame, int divs, long deadlineMillis) {
      this.first = frame;
      add(frame, divs, deadlineMillis);
    }

    private void add(Sl427Frame frame, int divs, long deadlineMillis) {
      byte[] body = frame.bodyRaw();
      fragments.add(body);
      sourceFrames.add(frame.rawFrame());
      totalBytes += body.length;
      previousDivs = divs;
      this.deadlineMillis = deadlineMillis;
    }
  }
}
