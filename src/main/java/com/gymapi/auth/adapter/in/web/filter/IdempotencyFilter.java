package com.gymapi.auth.adapter.in.web.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gymapi.auth.adapter.in.web.advice.ErrorResponseFactory;
import com.gymapi.auth.adapter.in.web.dto.generated.ErrorResponse;
import com.gymapi.auth.application.port.in.IdempotencyUseCase;
import com.gymapi.auth.application.port.in.IdempotencyUseCase.Outcome;
import com.gymapi.auth.domain.exception.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

/**
 * Makes mutating endpoints safe to retry.
 *
 * <p>Creating a role or assigning one is not naturally idempotent: a client that retries after a
 * timeout gets a 409 and cannot tell whether the first attempt landed. That matters more now that
 * the event publisher blocks on a broker ack and answers 503 when it does not arrive — a request
 * that is *meant* to be retried.
 *
 * <p>Opt-in per request. Send {@code Idempotency-Key} on a POST/PUT/PATCH/DELETE and the response
 * is recorded against it; any retry with the same key replays that response byte for byte and
 * carries {@code Idempotency-Replayed: true}, without touching the database or the broker a second
 * time. Requests without the header behave exactly as before.
 *
 * <p>Three cases are refused rather than guessed at:
 *
 * <ul>
 *   <li>the same key with a different request — the stored response would be the wrong answer
 *   <li>a key whose first attempt is still running — the outcome is not known yet
 *   <li>a malformed key — a silent fallthrough would look like idempotency was in effect
 * </ul>
 *
 * <p>5xx responses are not recorded: the claim is released so the retry genuinely re-runs.
 */
@Slf4j
@Component
@Order(IdempotencyFilter.ORDER)
@RequiredArgsConstructor
public class IdempotencyFilter extends OncePerRequestFilter {

  /** After Spring Security (-100), so an unauthenticated request never reserves a key. */
  static final int ORDER = Ordered.HIGHEST_PRECEDENCE + 200;

  public static final String HEADER_NAME = "Idempotency-Key";
  public static final String REPLAYED_HEADER = "Idempotency-Replayed";

  private static final Set<String> GUARDED_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");
  private static final int MAX_KEY_LENGTH = 255;

  private final IdempotencyUseCase idempotency;
  private final ErrorResponseFactory errorResponses;
  private final ObjectMapper objectMapper;

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    return !GUARDED_METHODS.contains(request.getMethod()) || request.getHeader(HEADER_NAME) == null;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    String key = request.getHeader(HEADER_NAME).trim();
    if (!isValidKey(key)) {
      writeError(
          request,
          response,
          ErrorCode.MALFORMED_REQUEST,
          "Idempotency-Key must be 1-"
              + MAX_KEY_LENGTH
              + " printable ASCII characters without whitespace");
      return;
    }

    byte[] body = StreamUtils.copyToByteArray(request.getInputStream());
    HttpServletRequest replayable = new CachedBodyHttpServletRequest(request, body);
    String fingerprint = fingerprint(request.getMethod(), request.getRequestURI(), body);

    Outcome outcome =
        idempotency.claim(key, request.getMethod(), request.getRequestURI(), fingerprint);

    switch (outcome) {
      case Outcome.Replay replay -> writeReplay(response, replay);
      case Outcome.KeyReused reused ->
          writeError(
              request,
              response,
              ErrorCode.IDEMPOTENCY_KEY_REUSED,
              "Idempotency-Key was already used for "
                  + reused.originalMethod()
                  + " "
                  + reused.originalPath()
                  + "; use a new key for a different request");
      case Outcome.InProgress ignored ->
          writeError(
              request,
              response,
              ErrorCode.IDEMPOTENT_REQUEST_IN_PROGRESS,
              "An earlier request with this Idempotency-Key is still in progress; retry shortly");
      case Outcome.Proceed ignored -> execute(key, replayable, response, filterChain);
    }
  }

  private void execute(
      String key, HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    ContentCachingResponseWrapper recorder = new ContentCachingResponseWrapper(response);
    try {
      filterChain.doFilter(request, recorder);
      record(key, recorder);
    } catch (ServletException | IOException | RuntimeException e) {
      // Nothing was recorded, so the client's retry runs for real rather than replaying a
      // response for work that may never have happened.
      idempotency.abandon(key);
      throw e;
    } finally {
      recorder.copyBodyToResponse();
    }
  }

  private void record(String key, ContentCachingResponseWrapper recorder) {
    int status = recorder.getStatus();
    if (status >= HttpStatus.INTERNAL_SERVER_ERROR.value()) {
      log.debug("Not recording {} for idempotency key {}; the retry should re-run", status, key);
      idempotency.abandon(key);
      return;
    }
    idempotency.complete(
        key,
        status,
        recorder.getContentType(),
        new String(recorder.getContentAsByteArray(), StandardCharsets.UTF_8));
  }

  private void writeReplay(HttpServletResponse response, Outcome.Replay replay) throws IOException {
    response.setStatus(replay.status());
    response.setHeader(REPLAYED_HEADER, "true");
    if (replay.contentType() != null) {
      response.setContentType(replay.contentType());
    }
    if (replay.body() != null && !replay.body().isEmpty()) {
      response.getOutputStream().write(replay.body().getBytes(StandardCharsets.UTF_8));
    }
  }

  private void writeError(
      HttpServletRequest request, HttpServletResponse response, ErrorCode code, String message)
      throws IOException {

    HttpStatus status = errorResponses.statusFor(code);
    ErrorResponse body = errorResponses.create(status, code, message, request);

    log.warn("{} {} -> {} {}", request.getRequestURI(), code, status.value(), message);
    response.setStatus(status.value());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    objectMapper.writeValue(response.getOutputStream(), body);
  }

  /**
   * Keys are opaque to us, but they end up in logs and in a length-bounded column, so anything
   * whitespace-laden, oversized or non-printable is refused up front.
   */
  private static boolean isValidKey(String key) {
    if (key.isEmpty() || key.length() > MAX_KEY_LENGTH) {
      return false;
    }
    return key.chars().allMatch(c -> c > 0x20 && c < 0x7F);
  }

  private static String fingerprint(String method, String path, byte[] body) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      digest.update(method.getBytes(StandardCharsets.UTF_8));
      digest.update((byte) '\n');
      digest.update(path.getBytes(StandardCharsets.UTF_8));
      digest.update((byte) '\n');
      digest.update(body);
      return HexFormat.of().formatHex(digest.digest());
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 is required by every JVM", e);
    }
  }
}
