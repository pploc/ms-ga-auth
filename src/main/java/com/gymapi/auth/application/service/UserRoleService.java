package com.gymapi.auth.application.service;

import com.gymapi.auth.application.port.in.UserRoleUseCase;
import com.gymapi.auth.application.port.out.UserRoleRepository;
import com.gymapi.auth.domain.exception.RoleNotFoundException;
import com.gymapi.auth.domain.exception.UserRoleNotFoundException;
import com.gymapi.auth.domain.model.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserRoleService implements UserRoleUseCase {

    private static final String USER_ROLE_NOT_FOUND = "User role not found";
    private static final String ROLE_NOT_FOUND = "Role not found with id: %s";

    private final UserRoleRepository userRoleRepository;
    private final RoleService roleService;

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
}
