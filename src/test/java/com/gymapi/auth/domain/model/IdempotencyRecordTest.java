package com.gymapi.auth.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

class IdempotencyRecordTest {

  private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-07-26T10:00:00Z");

  @Test
  void isCompletedReflectsTheState() {
    assertThat(record(IdempotencyState.COMPLETED, NOW).isCompleted()).isTrue();
    assertThat(record(IdempotencyState.IN_PROGRESS, NOW).isCompleted()).isFalse();
  }

  @Test
  void aClaimOlderThanTheCutoffCountsAsAbandoned() {
    assertThat(record(IdempotencyState.IN_PROGRESS, NOW.minusMinutes(5)).isAbandonedBy(NOW))
        .isTrue();
  }

  @Test
  void aRecentClaimIsNotAbandoned() {
    assertThat(record(IdempotencyState.IN_PROGRESS, NOW.plusSeconds(1)).isAbandonedBy(NOW))
        .isFalse();
  }

  /** A finished request is never "abandoned", however old its record is. */
  @Test
  void aCompletedRecordIsNeverAbandoned() {
    assertThat(record(IdempotencyState.COMPLETED, NOW.minusDays(2)).isAbandonedBy(NOW)).isFalse();
  }

  @Test
  void aRecordWithNoClaimTimeIsNotAbandoned() {
    assertThat(record(IdempotencyState.IN_PROGRESS, null).isAbandonedBy(NOW)).isFalse();
  }

  private static IdempotencyRecord record(IdempotencyState state, OffsetDateTime createdAt) {
    return IdempotencyRecord.builder()
        .key("key")
        .method("POST")
        .path("/auth/roles")
        .fingerprint("fp")
        .state(state)
        .createdAt(createdAt)
        .expiresAt(NOW.plusHours(24))
        .build();
  }
}
