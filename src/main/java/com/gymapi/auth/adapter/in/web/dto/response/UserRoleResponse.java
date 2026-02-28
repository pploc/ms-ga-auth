package com.gymapi.auth.adapter.in.web.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRoleResponse {
    private UUID id;
    private UUID userId;
    private UUID roleId;
    private String roleName;
    private UUID assignedBy;
    private OffsetDateTime assignedAt;
}
