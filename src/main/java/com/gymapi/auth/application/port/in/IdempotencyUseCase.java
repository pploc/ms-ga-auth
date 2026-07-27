package com.gymapi.auth.application.port.in;

/**
 * Claims and replays {@code Idempotency-Key} values so a client can retry a mutating request
 * without risking a second side effect.
 */
public interface IdempotencyUseCase {

  /**
   * Claims the key for this request.
   *
   * @param key the caller-supplied {@code Idempotency-Key}
   * @param method HTTP method, recorded so a replay can be checked against the original
   * @param path request path, likewise
   * @param fingerprint hash of method, path and body; distinguishes a genuine retry from a key
   *     accidentally reused for a different request
   */
  Outcome claim(String key, String method, String path, String fingerprint);

  /** Stores the response so later retries with the same key replay it. */
  void complete(String key, int status, String contentType, String body);

  /** Releases the claim, letting the client retry from scratch. */
  void abandon(String key);

  /** Deletes records past their retention window. Returns how many were removed. */
  int purgeExpired();

  /**
   * What the caller should do with the request, as a closed set so the filter's switch is checked
   * for exhaustiveness by the compiler.
   */
  sealed interface Outcome {

    /** The key is newly claimed; run the request. */
    record Proceed() implements Outcome {}

    /** The key has a stored response; return it instead of running the request again. */
    record Replay(int status, String contentType, String body) implements Outcome {}

    /** The key belongs to a different request. Reusing it would replay the wrong response. */
    record KeyReused(String originalMethod, String originalPath) implements Outcome {}

    /** An earlier attempt with this key is still running. */
    record InProgress() implements Outcome {}
  }
}
