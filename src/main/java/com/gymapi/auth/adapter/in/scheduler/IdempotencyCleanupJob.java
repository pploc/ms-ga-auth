package com.gymapi.auth.adapter.in.scheduler;

import com.gymapi.auth.application.port.in.IdempotencyUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Deletes idempotency records past their retention window.
 *
 * <p>Without this the table only grows: every mutating request that opts in leaves a row behind,
 * including the stored response body.
 *
 * <p>The interval comes from {@code gymapi.idempotency.purge-interval} and is wired up in {@code
 * ApplicationConfig} rather than with {@code @Scheduled}, because the annotation's string forms
 * only understand milliseconds or ISO-8601 and would reject the {@code 1h} the rest of the
 * configuration uses.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IdempotencyCleanupJob {

  private final IdempotencyUseCase idempotency;

  public void purgeExpiredRecords() {
    try {
      idempotency.purgeExpired();
    } catch (RuntimeException e) {
      // A failed purge must not kill the scheduler; the next tick tries again.
      log.error("Failed to purge expired idempotency records", e);
    }
  }
}
