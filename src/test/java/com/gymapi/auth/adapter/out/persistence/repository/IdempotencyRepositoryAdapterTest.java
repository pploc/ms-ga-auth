package com.gymapi.auth.adapter.out.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.gymapi.auth.adapter.out.persistence.entity.IdempotencyRecordEntity;
import com.gymapi.auth.domain.model.IdempotencyRecord;
import com.gymapi.auth.domain.model.IdempotencyState;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IdempotencyRepositoryAdapterTest {

  private static final String KEY = "key-1";

  @Mock private IdempotencyJpaRepository jpaRepository;

  @InjectMocks private IdempotencyRepositoryAdapter adapter;

  @Test
  void findByKeyMapsTheEntityToTheDomainRecord() {
    given(jpaRepository.findByIdempotencyKey(KEY)).willReturn(Optional.of(entity()));

    Optional<IdempotencyRecord> found = adapter.findByKey(KEY);

    assertThat(found).isPresent();
    IdempotencyRecord record = found.orElseThrow();
    assertThat(record.key()).isEqualTo(KEY);
    assertThat(record.method()).isEqualTo("POST");
    assertThat(record.path()).isEqualTo("/auth/roles");
    assertThat(record.fingerprint()).isEqualTo("fp");
    assertThat(record.state()).isEqualTo(IdempotencyState.IN_PROGRESS);
  }

  @Test
  void findByKeyIsEmptyForAnUnknownKey() {
    given(jpaRepository.findByIdempotencyKey(KEY)).willReturn(Optional.empty());

    assertThat(adapter.findByKey(KEY)).isEmpty();
  }

  /**
   * saveAndFlush, not save: a concurrent duplicate has to fail here so the caller can answer 409,
   * rather than at transaction commit where it is too late to respond.
   */
  @Test
  void insertFlushesSoADuplicateSurfacesImmediately() {
    IdempotencyRecord record =
        IdempotencyRecord.builder()
            .key(KEY)
            .method("POST")
            .path("/auth/roles")
            .fingerprint("fp")
            .state(IdempotencyState.IN_PROGRESS)
            .createdAt(OffsetDateTime.now())
            .expiresAt(OffsetDateTime.now().plusHours(24))
            .build();
    given(jpaRepository.saveAndFlush(any())).willReturn(entity());

    adapter.insert(record);

    ArgumentCaptor<IdempotencyRecordEntity> captor =
        ArgumentCaptor.forClass(IdempotencyRecordEntity.class);
    verify(jpaRepository).saveAndFlush(captor.capture());
    assertThat(captor.getValue().getIdempotencyKey()).isEqualTo(KEY);
    assertThat(captor.getValue().getState()).isEqualTo(IdempotencyState.IN_PROGRESS);
  }

  @Test
  void completeMutatesTheLoadedRowForDirtyChecking() {
    IdempotencyRecordEntity entity = entity();
    given(jpaRepository.findByIdempotencyKey(KEY)).willReturn(Optional.of(entity));

    adapter.complete(KEY, 201, "application/json", "{\"id\":1}");

    assertThat(entity.getState()).isEqualTo(IdempotencyState.COMPLETED);
    assertThat(entity.getResponseStatus()).isEqualTo(201);
    assertThat(entity.getResponseContentType()).isEqualTo("application/json");
    assertThat(entity.getResponseBody()).isEqualTo("{\"id\":1}");
  }

  @Test
  void completeIsAnoOpWhenTheClaimHasAlreadyGone() {
    given(jpaRepository.findByIdempotencyKey(KEY)).willReturn(Optional.empty());

    adapter.complete(KEY, 201, "application/json", "{}");
  }

  @Test
  void reclaimResetsTheStoredResponse() {
    IdempotencyRecordEntity entity = entity();
    entity.setState(IdempotencyState.COMPLETED);
    entity.setResponseStatus(500);
    entity.setResponseBody("stale");
    given(jpaRepository.findByIdempotencyKey(KEY)).willReturn(Optional.of(entity));

    OffsetDateTime claimedAt = OffsetDateTime.now();
    adapter.reclaim(KEY, "new-fp", claimedAt, claimedAt.plusHours(24));

    assertThat(entity.getState()).isEqualTo(IdempotencyState.IN_PROGRESS);
    assertThat(entity.getRequestFingerprint()).isEqualTo("new-fp");
    assertThat(entity.getCreatedAt()).isEqualTo(claimedAt);
    assertThat(entity.getResponseStatus()).isNull();
    assertThat(entity.getResponseContentType()).isNull();
    assertThat(entity.getResponseBody()).isNull();
  }

  @Test
  void reclaimIsAnoOpWhenTheClaimHasAlreadyGone() {
    given(jpaRepository.findByIdempotencyKey(KEY)).willReturn(Optional.empty());

    adapter.reclaim(KEY, "fp", OffsetDateTime.now(), OffsetDateTime.now().plusHours(1));
  }

  @Test
  void deleteByKeyDelegates() {
    adapter.deleteByKey(KEY);

    verify(jpaRepository).deleteByIdempotencyKey(KEY);
  }

  @Test
  void purgeReturnsTheDeletedCount() {
    OffsetDateTime cutoff = OffsetDateTime.now();
    given(jpaRepository.deleteByExpiresAtBefore(cutoff)).willReturn(4);

    assertThat(adapter.deleteExpiredBefore(cutoff)).isEqualTo(4);
  }

  private static IdempotencyRecordEntity entity() {
    return IdempotencyRecordEntity.builder()
        .id(UUID.randomUUID())
        .idempotencyKey(KEY)
        .requestMethod("POST")
        .requestPath("/auth/roles")
        .requestFingerprint("fp")
        .state(IdempotencyState.IN_PROGRESS)
        .createdAt(OffsetDateTime.now())
        .expiresAt(OffsetDateTime.now().plusHours(24))
        .build();
  }
}
