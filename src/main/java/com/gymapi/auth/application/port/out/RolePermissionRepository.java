package com.gymapi.auth.application.port.out;

import com.gymapi.auth.domain.model.Permission;
import java.util.List;
import java.util.UUID;

public interface RolePermissionRepository {
  List<Permission> findPermissionsByRoleId(UUID roleId);

  void saveRolePermissions(UUID roleId, List<UUID> permissionIds);

  void deleteAllByRoleId(UUID roleId);
}
