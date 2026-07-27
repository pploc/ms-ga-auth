package com.gymapi.auth.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.gymapi.auth.application.port.in.IdempotencyUseCase.Outcome;
import com.gymapi.auth.application.port.out.IdempotencyRepository;
import com.gymapi.auth.config.properties.IdempotencyProperties;
import com.gymapi.auth.domain.model.IdempotencyRecord;
import com.gymapi.auth.domain.model.IdempotencyState;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class IdempotencyServiceTest {

  private static final String KEY = "key-1";
  private static final String FINGERPRINT = "abc123";
  private static final Instant NOW = Instant.parse("2026-07-26T10:00:00Z");

  @Mock private IdempotencyRepository repository;

  private final IdempotencyProperties properties =
      new IdempotencyProperties(Duration.ofHours(24), Duration.ofSeconds(60), Duration.ofHours(1));
  private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

  private IdempotencyService service() {
    return new IdempotencyService(repository, properties, clock);
  }

  @Test
  void anUnusedKeyIsClaimedAndTheRequestProceeds() {
    given(repository.findByKey(KEY)).willReturn(Optional.empty());

    Outcome outcome = service().claim(KEY, "POST", "/auth/roles", FINGERPRINT);

    assertThat(outcome).isInstanceOf(Outcome.Proceed.class);

    ArgumentCaptor<IdempotencyRecord> captor = ArgumentCaptor.forClass(IdempotencyRecord.class);
    verify(repository).insert(captor.capture());
    IdempotencyRecord claimed = captor.getValue();
    assertThat(claimed.key()).isEqualTo(KEY);
    assertThat(claimed.state()).isEqualTo(IdempotencyState.IN_PROGRESS);
    assertThat(claimed.expiresAt())
        .isEqualTo(OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC).plusHours(24));
  }

  @Test
  void aCompletedKeyReplaysItsStoredResponse() {
    given(repository.findByKey(KEY)).willReturn(Optional.of(completed(201, "{\"id\":1}")));

    Outcome outcome = service().claim(KEY, "POST", "/auth/roles", FINGERPRINT);

    assertThat(outcome).isEqualTo(new Outcome.Replay(201, "application/json", "{\"id\":1}"));
    verify(repository, never()).insert(any());
  }

  @Test
  void aCompletedKeyWithNoRecordedStatusReplaysAs200() {
    given(repository.findByKey(KEY)).willReturn(Optional.of(completed(null, null)));

    Outcome outcome = service().claim(KEY, "DELETE", "/auth/roles/7", FINGERPRINT);

    assertThat(outcome).isEqualTo(new Outcome.Replay(200, "application/json", null));
  }

  @Test
  void aKeyUsedForADifferentRequestIsReported() {
    given(repository.findByKey(KEY)).willReturn(Optional.of(completed(201, "{}")));

    Outcome outcome = service().claim(KEY, "POST", "/auth/roles", "a-different-fingerprint");

    assertThat(outcome).isEqualTo(new Outcome.KeyReused("POST", "/auth/roles"));
  }

  @Test
  void aClaimThatIsStillRunningBlocksTheRetry() {
    given(repository.findByKey(KEY)).willReturn(Optional.of(inProgress(NOW.minusSeconds(5))));

    Outcome outcome = service().claim(KEY, "POST", "/auth/roles", FINGERPRINT);

    assertThat(outcome).isInstanceOf(Outcome.InProgress.class);
  }

  /** Otherwise a crash mid-request would lock the key out for the whole retention window. */
  @Test
  void aClaimWhoseHolderNeverFinishedIsTakenOver() {
    given(repository.findByKey(KEY)).willReturn(Optional.of(inProgress(NOW.minusSeconds(120))));

    Outcome outcome = service().claim(KEY, "POST", "/auth/roles", FINGERPRINT);

    assertThat(outcome).isInstanceOf(Outcome.Proceed.class);
    verify(repository)
        .reclaim(
            eq(KEY),
            eq(FINGERPRINT),
            eq(OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC)),
            eq(OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC).plusHours(24)));
  }

  /**
   * Two retries racing: the unique constraint decides, and the loser waits instead of re-running.
   */
  @Test
  void losingTheInsertRaceIsTreatedAsInProgressRatherThanFailing() {
    given(repository.findByKey(KEY)).willReturn(Optional.empty());
    given(repository.insert(any())).willThrow(new DataIntegrityViolationException("duplicate key"));

    Outcome outcome = service().claim(KEY, "POST", "/auth/roles", FINGERPRINT);

    assertThat(outcome).isInstanceOf(Outcome.InProgress.class);
  }

  @Test
  void completeStoresTheResponse() {
    service().complete(KEY, 201, "application/json", "{}");

    verify(repository).complete(KEY, 201, "application/json", "{}");
  }

  @Test
  void abandonReleasesTheClaim() {
    service().abandon(KEY);

    verify(repository).deleteByKey(KEY);
  }

  @Test
  void purgeReportsHowManyRecordsItRemoved() {
    given(repository.deleteExpiredBefore(OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC)))
        .willReturn(7);

    assertThat(service().purgeExpired()).isEqualTo(7);
  }

  @Test
  void purgeIsQuietWhenThereIsNothingToRemove() {
    given(repository.deleteExpiredBefore(any())).willReturn(0);

    assertThat(service().purgeExpired()).isZero();
  }

  private static IdempotencyRecord completed(Integer status, String body) {
    return base()
        .state(IdempotencyState.COMPLETED)
        .responseStatus(status)
        .responseContentType("application/json")
        .responseBody(body)
        .build();
  }

  private static IdempotencyRecord inProgress(Instant claimedAt) {
    return base()
        .state(IdempotencyState.IN_PROGRESS)
        .createdAt(OffsetDateTime.ofInstant(claimedAt, ZoneOffset.UTC))
        .build();
  }

  private static IdempotencyRecord.IdempotencyRecordBuilder base() {
    return IdempotencyRecord.builder()
        .key(KEY)
        .method("POST")
        .path("/auth/roles")
        .fingerprint(FINGERPRINT)
        .createdAt(OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC))
        .expiresAt(OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC).plusHours(24));
  }
}
