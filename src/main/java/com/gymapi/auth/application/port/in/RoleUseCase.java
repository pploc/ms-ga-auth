package com.gymapi.auth.application.port.in;

import com.gymapi.auth.domain.model.Permission;
import com.gymapi.auth.domain.model.Role;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface RoleUseCase {
    Role createRole(CreateRoleCommand command);

    Role getRole(UUID id);

    List<Role> getAllRoles();

    Role updateRole(UUID id, UpdateRoleCommand command);

    void deleteRole(UUID id);

    List<Permission> getRolePermissions(UUID roleId);

    void setRolePermissions(UUID roleId, Set<UUID> permissionIds);
}
