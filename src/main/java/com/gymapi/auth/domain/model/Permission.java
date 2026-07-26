package com.gymapi.auth.domain.model;

import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Builder;

@Builder(toBuilder = true)
public record Permission(
    UUID id, String resource, String action, String description, OffsetDateTime createdAt) {}
