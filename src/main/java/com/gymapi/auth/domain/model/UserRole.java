package com.gymapi.auth.domain.model;

import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Builder;

@Builder(toBuilder = true)
public record UserRole(
    UUID id,
    UUID userId,
    UUID roleId,
    /** Denormalised for reads; not set on the write path, where the role id is the source. */
    String roleName,
    UUID assignedBy,
    OffsetDateTime assignedAt) {}
