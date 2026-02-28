package com.gymapi.auth.application.service;

import com.gymapi.auth.application.port.in.UserRoleUseCase;
import com.gymapi.auth.application.port.out.RolePermissionRepository;
import com.gymapi.auth.application.port.out.UserRoleRepository;
import com.gymapi.auth.domain.exception.RoleNotFoundException;
import com.gymapi.auth.domain.exception.UserRoleNotFoundException;
import com.gymapi.auth.domain.model.Permission;
import com.gymapi.auth.domain.model.Role;
import com.gymapi.auth.domain.model.RolesWithPermissions;
import com.gymapi.auth.domain.model.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserRoleService implements UserRoleUseCase {

    private static final String USER_ROLE_NOT_FOUND = "User role not found";
    private static final String ROLE_NOT_FOUND = "Role not found with id: %s";

    private final UserRoleRepository userRoleRepository;
    private final RoleService roleService;
    private final RolePermissionRepository rolePermissionRepository;

    @Override
    @Transactional
    public UserRole assignRole(UUID userId, UUID roleId, UUID assignedBy) {
        roleService.getRoleById(roleId);

        if (userRoleRepository.existsByUserIdAndRoleId(userId, roleId)) {
            throw new IllegalStateException("User already has this role");
        }

        UserRole userRole = new UserRole(
                null,
                userId,
                roleId,
                assignedBy,
                OffsetDateTime.now()
        );

        return userRoleRepository.save(userRole);
    }

    @Override
    @Transactional
    public void revokeRole(UUID userId, UUID roleId) {
        UserRole userRole = userRoleRepository.findByUserIdAndRoleId(userId, roleId)
                .orElseThrow(() -> new UserRoleNotFoundException(USER_ROLE_NOT_FOUND));

        userRoleRepository.deleteByUserIdAndRoleId(userId, roleId);
    }

    @Override
    public List<UserRole> getUserRoles(UUID userId) {
        return userRoleRepository.findByUserId(userId);
    }

    @Override
    public List<UserRole> getRoleUsers(UUID roleId) {
        return userRoleRepository.findByRoleId(roleId);
    }

    @Override
    public boolean hasRole(UUID userId, UUID roleId) {
        return userRoleRepository.existsByUserIdAndRoleId(userId, roleId);
    }

    @Override
    public RolesWithPermissions getUserRolesWithPermissions(UUID userId) {
        List<UserRole> userRoles = userRoleRepository.findByUserId(userId);
        
        List<String> roleNames = new ArrayList<>();
        Set<String> permissions = new LinkedHashSet<>();
        
        for (UserRole userRole : userRoles) {
            try {
                Role role = roleService.getRoleById(userRole.roleId());
                roleNames.add(role.name());
                
                List<Permission> rolePermissions = rolePermissionRepository.findPermissionsByRoleId(userRole.roleId());
                for (Permission permission : rolePermissions) {
                    String permString = permission.resource() + ":" + permission.action();
                    permissions.add(permString);
                }
            } catch (RoleNotFoundException e) {
                // Skip if role not found
            }
        }
        
        return new RolesWithPermissions(userId, roleNames, new ArrayList<>(permissions));
    }
}
