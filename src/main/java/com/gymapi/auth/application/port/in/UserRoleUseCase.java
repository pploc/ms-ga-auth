package com.gymapi.auth.application.port.in;

import com.gymapi.auth.domain.model.RolesWithPermissions;
import com.gymapi.auth.domain.model.UserRole;
import java.util.List;
import java.util.UUID;

public interface UserRoleUseCase {
  UserRole assignRole(UUID userId, UUID roleId, UUID assignedBy);

  void revokeRole(UUID userId, UUID roleId);

  List<UserRole> getUserRoles(UUID userId);

  List<UserRole> getRoleUsers(UUID roleId);

  boolean hasRole(UUID userId, UUID roleId);

  RolesWithPermissions getUserRolesWithPermissions(UUID userId);
}
