package com.gymapi.auth.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RolesWithPermissions {
    private UUID userId;
    private List<String> roles;
    private List<String> permissions;
}
