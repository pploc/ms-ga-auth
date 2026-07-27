package com.gymapi.auth.application.port.out;

public interface EventPublisher {
  void publishRoleAssigned(String userId, String roleId, String assignedBy);

  void publishRoleRevoked(String userId, String roleId);

  void publishPermissionChanged(String roleId, String roleName, String changeType);
}
