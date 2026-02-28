package com.gymapi.auth.application.service;

import com.gymapi.auth.application.port.in.AssignRoleCommand;
import com.gymapi.auth.application.port.in.UserRoleUseCase;
import com.gymapi.auth.application.port.out.EventPublisher;
import com.gymapi.auth.application.port.out.RoleRepository;
import com.gymapi.auth.application.port.out.UserRoleRepository;
import com.gymapi.auth.domain.exception.ConflictException;
import com.gymapi.auth.domain.exception.ResourceNotFoundException;
import com.gymapi.auth.domain.model.Role;
import com.gymapi.auth.domain.model.RolesWithPermissions;
import com.gymapi.auth.domain.model.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserRoleService implements UserRoleUseCase {

    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;
    private final EventPublisher eventPublisher;

    @Override
    @Transactional
    public void assignRole(AssignRoleCommand command) {
        Role role = roleRepository.findById(command.getRoleId())
                .orElseThrow(() -> new ResourceNotFoundException("Role not found"));

        if (userRoleRepository.existsByUserIdAndRoleId(command.getUserId(), command.getRoleId())) {
            throw new ConflictException("User already has this role");
        }

        UserRole userRole = UserRole.builder()
                .userId(command.getUserId())
                .roleId(command.getRoleId())
                .assignedBy(command.getAssignedBy())
                .build();

        userRoleRepository.save(userRole);
        eventPublisher.publishRoleAssignedEvent(command.getUserId(), command.getRoleId(), role.getName(),
                command.getAssignedBy());
    }

    @Override
    @Transactional
    public void removeRole(UUID userId, UUID roleId) {
        if (!userRoleRepository.existsByUserIdAndRoleId(userId, roleId)) {
            throw new ResourceNotFoundException("User does not have this role");
        }

        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found"));

        userRoleRepository.deleteByUserIdAndRoleId(userId, roleId);
        eventPublisher.publishRoleRevokedEvent(userId, roleId, role.getName());
    }

    @Override
    public List<Role> getUserRoles(UUID userId) {
        return userRoleRepository.findRolesByUserId(userId);
    }

    @Override
    public RolesWithPermissions getUserRolesWithPermissions(UUID userId) {
        return userRoleRepository.findRolesWithPermissionsByUserId(userId);
    }
}
