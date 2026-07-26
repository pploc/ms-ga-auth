package com.gymapi.auth.domain.model;

import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Builder;

@Builder(toBuilder = true)
public record Role(
    UUID id,
    String name,
    String description,
    boolean isSystem,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt) {}
