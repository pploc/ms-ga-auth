package com.gymapi.auth.application.port.in;

import com.gymapi.auth.domain.model.Permission;

import java.util.List;
import java.util.UUID;

public interface PermissionUseCase {
    Permission createPermission(String resource, String action, String description);
    Permission updatePermission(UUID id, String resource, String action, String description);
    void deletePermission(UUID id);
    Permission getPermissionById(UUID id);
    Permission getPermissionByResourceAndAction(String resource, String action);
    List<Permission> getAllPermissions();
}
