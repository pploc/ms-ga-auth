package com.gymapi.auth.domain.model;

import java.time.OffsetDateTime;
import java.util.UUID;

public record UserRole(
        UUID id,
        UUID userId,
        UUID roleId,
        UUID assignedBy,
        OffsetDateTime assignedAt
) {
}
