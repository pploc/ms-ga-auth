package com.gymapi.auth.application.port.out;

import com.gymapi.auth.domain.model.IdempotencyRecord;
import java.time.OffsetDateTime;
import java.util.Optional;

public interface IdempotencyRepository {

  Optional<IdempotencyRecord> findByKey(String key);

  /**
   * Inserts a claim. Callers rely on the unique constraint on the key: a concurrent duplicate must
   * fail rather than silently produce a second row, which is what makes racing retries safe.
   */
  IdempotencyRecord insert(IdempotencyRecord record);

  void complete(String key, int status, String contentType, String body);

  /** Re-claims a key whose previous holder never finished, so a retry can proceed. */
  void reclaim(String key, String fingerprint, OffsetDateTime claimedAt, OffsetDateTime expiresAt);

  void deleteByKey(String key);

  int deleteExpiredBefore(OffsetDateTime cutoff);
}
