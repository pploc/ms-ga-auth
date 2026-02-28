package com.gymapi.auth.application.port.in;

import com.gymapi.auth.domain.model.Role;

import java.util.List;
import java.util.UUID;

public interface RoleUseCase {
    Role createRole(String name, String description, boolean isSystem);
    Role updateRole(UUID id, String name, String description);
    void deleteRole(UUID id);
    Role getRoleById(UUID id);
    Role getRoleByName(String name);
    List<Role> getAllRoles();
}
