package com.gymapi.auth.application.port.out;

import java.util.UUID;

public interface EventPublisher {
    void publishRoleAssignedEvent(UUID userId, UUID roleId, String roleName, UUID assignedBy);

    void publishRoleRevokedEvent(UUID userId, UUID roleId, String roleName);

    void publishPermissionChangedEvent(UUID roleId, String roleName);
}
