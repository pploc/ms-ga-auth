package com.gymapi.auth.domain.model;

import java.time.OffsetDateTime;
import java.util.UUID;

public record Role(
        UUID id,
        String name,
        String description,
        boolean isSystem,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
