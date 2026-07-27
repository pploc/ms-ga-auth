package com.gymapi.auth.domain.model;

import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Builder;

/**
 * A claimed {@code Idempotency-Key} and, once the request finishes, the response to replay for it.
 */
@Builder(toBuilder = true)
public record IdempotencyRecord(
    UUID id,
    String key,
    String method,
    String path,
    String fingerprint,
    IdempotencyState state,
    Integer responseStatus,
    String responseContentType,
    String responseBody,
    OffsetDateTime createdAt,
    OffsetDateTime expiresAt) {

  public boolean isCompleted() {
    return state == IdempotencyState.COMPLETED;
  }

  /**
   * True when the key was claimed by a request that never reported back — a crash mid-flight, say.
   * Such a claim is taken over rather than blocking the key until it expires.
   */
  public boolean isAbandonedBy(OffsetDateTime cutoff) {
    return state == IdempotencyState.IN_PROGRESS && createdAt != null && createdAt.isBefore(cutoff);
  }
}
