package com.gymapi.auth.adapter.in.scheduler;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.gymapi.auth.application.port.in.IdempotencyUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IdempotencyCleanupJobTest {

  @Mock private IdempotencyUseCase idempotency;

  @InjectMocks private IdempotencyCleanupJob job;

  @Test
  void purgesOnEachRun() {
    given(idempotency.purgeExpired()).willReturn(3);

    job.purgeExpiredRecords();

    verify(idempotency).purgeExpired();
  }

  /** A failure here must not stop the scheduler; the next tick has to get its turn. */
  @Test
  void swallowsAFailureSoTheScheduleSurvives() {
    given(idempotency.purgeExpired()).willThrow(new IllegalStateException("database is down"));

    assertThatCode(job::purgeExpiredRecords).doesNotThrowAnyException();
  }
}
