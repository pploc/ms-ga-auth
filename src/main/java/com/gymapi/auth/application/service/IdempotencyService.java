package com.gymapi.auth.application.service;

import com.gymapi.auth.application.port.in.IdempotencyUseCase;
import com.gymapi.auth.application.port.out.IdempotencyRepository;
import com.gymapi.auth.config.properties.IdempotencyProperties;
import com.gymapi.auth.domain.model.IdempotencyRecord;
import com.gymapi.auth.domain.model.IdempotencyState;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Bookkeeping behind the {@code Idempotency-Key} header.
 *
 * <p>Every method runs in its own transaction ({@code REQUIRES_NEW}). That is the point: the claim
 * has to be visible to a concurrent retry before the request it guards has finished, and the stored
 * response has to survive a rollback of that request — otherwise a retry of a failed call would
 * replay a response describing work that was undone.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IdempotencyService implements IdempotencyUseCase {

  private final IdempotencyRepository repository;
  private final IdempotencyProperties properties;
  private final Clock clock;

  @Override
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public Outcome claim(String key, String method, String path, String fingerprint) {
    OffsetDateTime now = OffsetDateTime.now(clock);
    Optional<IdempotencyRecord> existing = repository.findByKey(key);

    if (existing.isPresent()) {
      return evaluate(existing.get(), key, method, path, fingerprint, now);
    }

    try {
      repository.insert(newClaim(key, method, path, fingerprint, now));
      return new Outcome.Proceed();
    } catch (DataIntegrityViolationException e) {
      // Another request inserted the same key between the lookup and the insert. The unique
      // constraint is what makes that race safe; the loser waits rather than double-executing.
      log.debug("Idempotency key {} was claimed concurrently", key);
      return new Outcome.InProgress();
    }
  }

  private Outcome evaluate(
      IdempotencyRecord record,
      String key,
      String method,
      String path,
      String fingerprint,
      OffsetDateTime now) {

    if (!record.fingerprint().equals(fingerprint)) {
      return new Outcome.KeyReused(record.method(), record.path());
    }
    if (record.isCompleted()) {
      log.debug("Replaying stored response for idempotency key {}", key);
      return new Outcome.Replay(
          record.responseStatus() == null ? 200 : record.responseStatus(),
          record.responseContentType(),
          record.responseBody());
    }
    if (record.isAbandonedBy(now.minus(properties.inProgressTimeout()))) {
      log.warn("Taking over idempotency key {}, previous attempt never completed", key);
      repository.reclaim(key, fingerprint, now, now.plus(properties.retention()));
      return new Outcome.Proceed();
    }
    return new Outcome.InProgress();
  }

  @Override
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void complete(String key, int status, String contentType, String body) {
    repository.complete(key, status, contentType, body);
  }

  @Override
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void abandon(String key) {
    repository.deleteByKey(key);
  }

  @Override
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public int purgeExpired() {
    int removed = repository.deleteExpiredBefore(OffsetDateTime.now(clock));
    if (removed > 0) {
      log.info("Purged {} expired idempotency records", removed);
    }
    return removed;
  }

  private IdempotencyRecord newClaim(
      String key, String method, String path, String fingerprint, OffsetDateTime now) {
    return IdempotencyRecord.builder()
        .key(key)
        .method(method)
        .path(path)
        .fingerprint(fingerprint)
        .state(IdempotencyState.IN_PROGRESS)
        .createdAt(now)
        .expiresAt(now.plus(properties.retention()))
        .build();
  }
}
