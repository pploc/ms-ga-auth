package com.gymapi.auth.application.port.out;

import com.gymapi.auth.domain.model.Role;
import com.gymapi.auth.domain.model.RolesWithPermissions;
import com.gymapi.auth.domain.model.UserRole;

import java.util.List;
import java.util.UUID;

public interface UserRoleRepository {
    UserRole save(UserRole userRole);

    void deleteByUserIdAndRoleId(UUID userId, UUID roleId);

    List<Role> findRolesByUserId(UUID userId);

    boolean existsByUserIdAndRoleId(UUID userId, UUID roleId);

    RolesWithPermissions findRolesWithPermissionsByUserId(UUID userId);
}
