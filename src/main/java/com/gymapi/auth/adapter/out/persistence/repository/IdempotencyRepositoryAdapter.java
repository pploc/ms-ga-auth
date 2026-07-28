package com.gymapi.auth.adapter.out.persistence.repository;

import com.gymapi.auth.adapter.out.persistence.entity.IdempotencyRecordEntity;
import com.gymapi.auth.application.port.out.IdempotencyRepository;
import com.gymapi.auth.domain.model.IdempotencyRecord;
import com.gymapi.auth.domain.model.IdempotencyState;
import java.time.OffsetDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class IdempotencyRepositoryAdapter implements IdempotencyRepository {

  private final IdempotencyJpaRepository jpaRepository;

  @Override
  public Optional<IdempotencyRecord> findByKey(String key) {
    return jpaRepository.findByIdempotencyKey(key).map(IdempotencyRepositoryAdapter::toDomain);
  }

  @Override
  public IdempotencyRecord insert(IdempotencyRecord record) {
    // saveAndFlush, not save: the unique-constraint violation must surface here rather than at
    // transaction commit, so the caller can turn a concurrent duplicate into a 409.
    return toDomain(jpaRepository.saveAndFlush(toEntity(record)));
  }

  // The two mutators below load the row and change it. Inside the caller's transaction Hibernate
  // writes the change back on commit, so no update statement has to be spelled out here.

  @Override
  public void complete(String key, int status, String contentType, String body) {
    jpaRepository
        .findByIdempotencyKey(key)
        .ifPresent(
            record -> {
              record.setState(IdempotencyState.COMPLETED);
              record.setResponseStatus(status);
              record.setResponseContentType(contentType);
              record.setResponseBody(body);
            });
  }

  @Override
  public void reclaim(
      String key, String fingerprint, OffsetDateTime claimedAt, OffsetDateTime expiresAt) {
    jpaRepository
        .findByIdempotencyKey(key)
        .ifPresent(
            record -> {
              record.setState(IdempotencyState.IN_PROGRESS);
              record.setRequestFingerprint(fingerprint);
              record.setCreatedAt(claimedAt);
              record.setExpiresAt(expiresAt);
              record.setResponseStatus(null);
              record.setResponseContentType(null);
              record.setResponseBody(null);
            });
  }

  @Override
  public void deleteByKey(String key) {
    jpaRepository.deleteByIdempotencyKey(key);
  }

  @Override
  public int deleteExpiredBefore(OffsetDateTime cutoff) {
    return jpaRepository.deleteByExpiresAtBefore(cutoff);
  }

  private static IdempotencyRecordEntity toEntity(IdempotencyRecord record) {
    return IdempotencyRecordEntity.builder()
        .id(record.id())
        .idempotencyKey(record.key())
        .requestMethod(record.method())
        .requestPath(record.path())
        .requestFingerprint(record.fingerprint())
        .state(record.state())
        .responseStatus(record.responseStatus())
        .responseContentType(record.responseContentType())
        .responseBody(record.responseBody())
        .createdAt(record.createdAt())
        .expiresAt(record.expiresAt())
        .build();
  }

  private static IdempotencyRecord toDomain(IdempotencyRecordEntity entity) {
    return IdempotencyRecord.builder()
        .id(entity.getId())
        .key(entity.getIdempotencyKey())
        .method(entity.getRequestMethod())
        .path(entity.getRequestPath())
        .fingerprint(entity.getRequestFingerprint())
        .state(entity.getState())
        .responseStatus(entity.getResponseStatus())
        .responseContentType(entity.getResponseContentType())
        .responseBody(entity.getResponseBody())
        .createdAt(entity.getCreatedAt())
        .expiresAt(entity.getExpiresAt())
        .build();
  }
}
