package com.gymapi.auth.application.port.in;

import com.gymapi.auth.domain.model.Permission;

import java.util.List;
import java.util.UUID;

public interface PermissionUseCase {
    Permission createPermission(CreatePermissionCommand command);

    List<Permission> getAllPermissions();

    Permission updatePermission(UUID id, UpdatePermissionCommand command);

    void deletePermission(UUID id);
}
