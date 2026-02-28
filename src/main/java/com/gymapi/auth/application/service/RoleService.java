package com.gymapi.auth.application.service;

import com.gymapi.auth.application.port.in.CreateRoleCommand;
import com.gymapi.auth.application.port.in.RoleUseCase;
import com.gymapi.auth.application.port.in.UpdateRoleCommand;
import com.gymapi.auth.application.port.out.EventPublisher;
import com.gymapi.auth.application.port.out.PermissionRepository;
import com.gymapi.auth.application.port.out.RoleRepository;
import com.gymapi.auth.domain.exception.ConflictException;
import com.gymapi.auth.domain.exception.ForbiddenException;
import com.gymapi.auth.domain.exception.ResourceNotFoundException;
import com.gymapi.auth.domain.model.Permission;
import com.gymapi.auth.domain.model.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RoleService implements RoleUseCase {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final EventPublisher eventPublisher;

    @Override
    @Transactional
    public Role createRole(CreateRoleCommand command) {
        if (roleRepository.existsByName(command.getName())) {
            throw new ConflictException("Role name already exists");
        }

        Role role = Role.builder()
                .name(command.getName())
                .description(command.getDescription())
                .system(false)
                .build();

        return roleRepository.save(role);
    }

    @Override
    public Role getRole(UUID id) {
        return roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found"));
    }

    @Override
    public List<Role> getAllRoles() {
        return roleRepository.findAll();
    }

    @Override
    @Transactional
    public Role updateRole(UUID id, UpdateRoleCommand command) {
        Role role = getRole(id);

        if (role.isSystem() && command.getName() != null && !role.getName().equals(command.getName())) {
            throw new ForbiddenException("Cannot modify system role name");
        }

        if (command.getName() != null && !role.getName().equals(command.getName())) {
            if (roleRepository.existsByName(command.getName())) {
                throw new ConflictException("Role name already exists");
            }
            role.setName(command.getName());
        }

        if (command.getDescription() != null) {
            role.setDescription(command.getDescription());
        }

        return roleRepository.save(role);
    }

    @Override
    @Transactional
    public void deleteRole(UUID id) {
        Role role = getRole(id);

        if (role.isSystem()) {
            throw new ForbiddenException("Cannot delete system role");
        }

        // Let DB handle ConstraintViolationException (HTTP 409) if role is assigned to
        // users,
        // or we could explicitly check via UserRoleRepository here.

        roleRepository.deleteById(id);
    }

    @Override
    public List<Permission> getRolePermissions(UUID roleId) {
        return getRole(roleId).getPermissions();
    }

    @Override
    @Transactional
    public void setRolePermissions(UUID roleId, Set<UUID> permissionIds) {
        Role role = getRole(roleId);
        List<Permission> permissions = permissionRepository.findAllByIds(permissionIds);

        role.setPermissions(permissions);
        roleRepository.save(role);

        eventPublisher.publishPermissionChangedEvent(roleId, role.getName());
    }
}
