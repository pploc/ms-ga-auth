package com.gymapi.auth.application.port.in;

import com.gymapi.auth.domain.model.Role;
import com.gymapi.auth.domain.model.RolesWithPermissions;

import java.util.List;
import java.util.UUID;

public interface UserRoleUseCase {
    void assignRole(AssignRoleCommand command);

    void removeRole(UUID userId, UUID roleId);

    List<Role> getUserRoles(UUID userId);

    RolesWithPermissions getUserRolesWithPermissions(UUID userId);
}
