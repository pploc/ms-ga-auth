package com.gymapi.auth.adapter.in.web.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignRoleRequest {
    @NotNull(message = "Role ID is required")
    private UUID roleId;

    private UUID assignedBy;
}
