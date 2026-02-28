package com.gymapi.auth.application.port.in;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignRoleCommand {
    private UUID userId;
    private UUID roleId;
    private UUID assignedBy;
}
