package com.gymapi.auth.domain.model;

import java.util.List;
import java.util.UUID;
import lombok.Builder;

@Builder(toBuilder = true)
public record RolesWithPermissions(UUID userId, List<String> roles, List<String> permissions) {}
