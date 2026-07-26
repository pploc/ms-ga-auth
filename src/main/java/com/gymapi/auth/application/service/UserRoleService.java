package com.gymapi.auth.application.service;

import com.gymapi.auth.application.port.in.UserRoleUseCase;
import com.gymapi.auth.application.port.out.EventPublisher;
import com.gymapi.auth.application.port.out.RolePermissionRepository;
import com.gymapi.auth.application.port.out.RoleRepository;
import com.gymapi.auth.application.port.out.UserRoleRepository;
import com.gymapi.auth.domain.exception.DuplicateUserRoleException;
import com.gymapi.auth.domain.exception.RoleNotFoundException;
import com.gymapi.auth.domain.exception.UserRoleNotFoundException;
import com.gymapi.auth.domain.model.Permission;
import com.gymapi.auth.domain.model.Role;
import com.gymapi.auth.domain.model.RolesWithPermissions;
import com.gymapi.auth.domain.model.UserRole;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserRoleService implements UserRoleUseCase {

  private final UserRoleRepository userRoleRepository;
  private final RoleRepository roleRepository;
  private final RolePermissionRepository rolePermissionRepository;
  private final EventPublisher eventPublisher;

  @Override
  @Transactional
  public UserRole assignRole(UUID userId, UUID roleId, UUID assignedBy) {
    if (roleRepository.findById(roleId).isEmpty()) {
      throw RoleNotFoundException.byId(roleId);
    }
    if (userRoleRepository.existsByUserIdAndRoleId(userId, roleId)) {
      throw DuplicateUserRoleException.of(userId, roleId);
    }

    UserRole saved =
        userRoleRepository.save(
            UserRole.builder()
                .userId(userId)
                .roleId(roleId)
                .assignedBy(assignedBy)
                .assignedAt(OffsetDateTime.now())
                .build());

    eventPublisher.publishRoleAssigned(
        userId.toString(), roleId.toString(), assignedBy == null ? null : assignedBy.toString());
    return saved;
  }

  @Override
  @Transactional
  public void revokeRole(UUID userId, UUID roleId) {
    userRoleRepository
        .findByUserIdAndRoleId(userId, roleId)
        .orElseThrow(() -> UserRoleNotFoundException.of(userId, roleId));

    userRoleRepository.deleteByUserIdAndRoleId(userId, roleId);
    eventPublisher.publishRoleRevoked(userId.toString(), roleId.toString());
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

  /**
   * Flattens the user's assignments into the shape {@code ms-ga-identifier} embeds in the JWT.
   *
   * <p>An assignment pointing at a role that no longer exists is skipped rather than failing the
   * login it is feeding — but it is logged, because it means a delete left an orphan behind.
   */
  @Override
  public RolesWithPermissions getUserRolesWithPermissions(UUID userId) {
    List<String> roleNames = new ArrayList<>();
    Set<String> permissions = new LinkedHashSet<>();

    for (UserRole userRole : userRoleRepository.findByUserId(userId)) {
      Optional<Role> role = roleRepository.findById(userRole.roleId());
      if (role.isEmpty()) {
        log.warn("User {} is assigned role {}, which no longer exists", userId, userRole.roleId());
        continue;
      }

      roleNames.add(role.get().name());
      for (Permission permission :
          rolePermissionRepository.findPermissionsByRoleId(userRole.roleId())) {
        permissions.add(permission.resource() + ":" + permission.action());
      }
    }

    return RolesWithPermissions.builder()
        .userId(userId)
        .roles(roleNames)
        .permissions(new ArrayList<>(permissions))
        .build();
  }
}
