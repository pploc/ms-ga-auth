package com.gymapi.auth.domain.model;

/** Lifecycle of a recorded {@code Idempotency-Key}. */
public enum IdempotencyState {

  /** Claimed by a request that has not finished yet. A concurrent retry must be turned away. */
  IN_PROGRESS,

  /** The response is stored and any retry with this key replays it. */
  COMPLETED
}
