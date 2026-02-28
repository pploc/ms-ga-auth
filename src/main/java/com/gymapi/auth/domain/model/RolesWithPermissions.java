package com.gymapi.auth.domain.model;

import java.util.List;
import java.util.UUID;

public record RolesWithPermissions(
        UUID userId,
        List<String> roles,
        List<String> permissions
) {
}
