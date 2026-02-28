package com.gymapi.auth.application.service;

import com.gymapi.auth.application.port.in.RoleUseCase;
import com.gymapi.auth.application.port.out.RolePermissionRepository;
import com.gymapi.auth.application.port.out.RoleRepository;
import com.gymapi.auth.domain.exception.SystemRoleDeletionException;
import com.gymapi.auth.domain.exception.DuplicateRoleException;
import com.gymapi.auth.domain.exception.RoleNotFoundException;
import com.gymapi.auth.domain.model.Permission;
import com.gymapi.auth.domain.model.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RoleService implements RoleUseCase {

    private static final String SYSTEM_ROLE_CANNOT_BE_DELETED = "System role cannot be deleted";
    private static final String SYSTEM_ROLE_CANNOT_BE_UPDATED = "System role cannot be updated";
    private static final String ROLE_ALREADY_EXISTS = "Role with name '%s' already exists";
    private static final String ROLE_NOT_FOUND = "Role not found with id: %s";

    private final RoleRepository roleRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final com.gymapi.auth.application.port.out.EventPublisher eventPublisher;

    @Override
    @Transactional
    public Role createRole(String name, String description, boolean isSystem) {
        if (roleRepository.existsByName(name)) {
            throw new DuplicateRoleException(String.format(ROLE_ALREADY_EXISTS, name));
        }

        Role role = new Role(
                null,
                name,
                description,
                isSystem,
                OffsetDateTime.now(),
                OffsetDateTime.now());

        return roleRepository.save(role);
    }

    @Override
    @Transactional
    public Role updateRole(UUID id, String name, String description) {
        Role existingRole = roleRepository.findById(id)
                .orElseThrow(() -> new RoleNotFoundException(String.format(ROLE_NOT_FOUND, id)));

        if (existingRole.isSystem()) {
            throw new SystemRoleDeletionException(SYSTEM_ROLE_CANNOT_BE_UPDATED);
        }

        if (!existingRole.name().equals(name) && roleRepository.existsByName(name)) {
            throw new DuplicateRoleException(String.format(ROLE_ALREADY_EXISTS, name));
        }

        Role updatedRole = new Role(
                existingRole.id(),
                name,
                description,
                existingRole.isSystem(),
                existingRole.createdAt(),
                OffsetDateTime.now());

        return roleRepository.save(updatedRole);
    }

    @Override
    @Transactional
    public void deleteRole(UUID id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new RoleNotFoundException(String.format(ROLE_NOT_FOUND, id)));

        if (role.isSystem()) {
            throw new SystemRoleDeletionException(SYSTEM_ROLE_CANNOT_BE_DELETED);
        }

        roleRepository.deleteById(id);
    }

    @Override
    public Role getRoleById(UUID id) {
        return roleRepository.findById(id)
                .orElseThrow(() -> new RoleNotFoundException(String.format(ROLE_NOT_FOUND, id)));
    }

    @Override
    public Role getRoleByName(String name) {
        return roleRepository.findByName(name)
                .orElseThrow(() -> new RoleNotFoundException("Role not found with name: " + name));
    }

    @Override
    public List<Role> getAllRoles() {
        return roleRepository.findAll();
    }

    @Override
    public List<Permission> getRolePermissions(UUID roleId) {
        roleRepository.findById(roleId)
                .orElseThrow(() -> new RoleNotFoundException(String.format(ROLE_NOT_FOUND, roleId)));
        return rolePermissionRepository.findPermissionsByRoleId(roleId);
    }

    @Override
    @Transactional
    public void setRolePermissions(UUID roleId, Set<UUID> permissionIds) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RoleNotFoundException(String.format(ROLE_NOT_FOUND, roleId)));

        if (role.isSystem()) {
            throw new SystemRoleDeletionException("Cannot modify permissions of system role");
        }

        rolePermissionRepository.saveRolePermissions(roleId, List.copyOf(permissionIds));
        eventPublisher.publishPermissionChanged(roleId.toString(), role.name(), "permissions_updated");
    }
}
