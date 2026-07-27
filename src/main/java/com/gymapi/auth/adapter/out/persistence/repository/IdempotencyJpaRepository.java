package com.gymapi.auth.adapter.out.persistence.repository;

import com.gymapi.auth.adapter.out.persistence.entity.IdempotencyRecordEntity;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Derived queries only.
 *
 * <p>Updates are done by loading the row and mutating it — Hibernate's dirty checking writes it
 * back on commit. A bulk {@code @Modifying} statement would be one fewer round trip but writes
 * behind the persistence context's back, which is why the earlier version of this interface needed
 * {@code clearAutomatically}/{@code flushAutomatically} to stay consistent. For single rows already
 * inside the transaction, that complexity buys nothing.
 */
@Repository
public interface IdempotencyJpaRepository extends JpaRepository<IdempotencyRecordEntity, UUID> {

  Optional<IdempotencyRecordEntity> findByIdempotencyKey(String idempotencyKey);

  void deleteByIdempotencyKey(String idempotencyKey);

  /**
   * Derived delete: loads the expired rows and removes them, returning the count.
   *
   * <p>Row-at-a-time is the right default at this table's size — one record per opted-in mutating
   * request, kept for a day. If the purge ever starts moving tens of thousands of rows per run,
   * this single method is the place to drop down to a bulk delete.
   */
  int deleteByExpiresAtBefore(OffsetDateTime cutoff);
}
