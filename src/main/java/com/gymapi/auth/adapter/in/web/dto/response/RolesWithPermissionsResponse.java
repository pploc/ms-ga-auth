package com.gymapi.auth.adapter.in.web.dto.response;

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
public class RolesWithPermissionsResponse {
    private UUID userId;
    private List<String> roles;
    private List<String> permissions;
}
