package com.gymapi.auth.application.service;

import com.gymapi.auth.application.port.in.RoleUseCase;
import com.gymapi.auth.application.port.out.EventPublisher;
import com.gymapi.auth.application.port.out.RolePermissionRepository;
import com.gymapi.auth.application.port.out.RoleRepository;
import com.gymapi.auth.domain.exception.DuplicateRoleException;
import com.gymapi.auth.domain.exception.RoleNotFoundException;
import com.gymapi.auth.domain.exception.SystemRoleModificationException;
import com.gymapi.auth.domain.model.Permission;
import com.gymapi.auth.domain.model.Role;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoleService implements RoleUseCase {

  private final RoleRepository roleRepository;
  private final RolePermissionRepository rolePermissionRepository;
  private final EventPublisher eventPublisher;

  @Override
  @Transactional
  public Role createRole(String name, String description, boolean isSystem) {
    if (roleRepository.existsByName(name)) {
      throw DuplicateRoleException.byName(name);
    }

    OffsetDateTime now = OffsetDateTime.now();
    Role role =
        Role.builder()
            .name(name)
            .description(description)
            .isSystem(isSystem)
            .createdAt(now)
            .updatedAt(now)
            .build();

    return roleRepository.save(role);
  }

  @Override
  @Transactional
  public Role updateRole(UUID id, String name, String description) {
    Role existing = requireRole(id);

    if (existing.isSystem()) {
      throw SystemRoleModificationException.cannotUpdate(existing.name());
    }
    if (!existing.name().equals(name) && roleRepository.existsByName(name)) {
      throw DuplicateRoleException.byName(name);
    }

    return roleRepository.save(
        existing.toBuilder()
            .name(name)
            .description(description)
            .updatedAt(OffsetDateTime.now())
            .build());
  }

  @Override
  @Transactional
  public void deleteRole(UUID id) {
    Role role = requireRole(id);

    if (role.isSystem()) {
      throw SystemRoleModificationException.cannotDelete(role.name());
    }

    roleRepository.deleteById(id);
  }

  @Override
  public Role getRoleById(UUID id) {
    return requireRole(id);
  }

  @Override
  public Role getRoleByName(String name) {
    return roleRepository.findByName(name).orElseThrow(() -> RoleNotFoundException.byName(name));
  }

  @Override
  public List<Role> getAllRoles() {
    return roleRepository.findAll();
  }

  @Override
  public List<Permission> getRolePermissions(UUID roleId) {
    requireRole(roleId);
    return rolePermissionRepository.findPermissionsByRoleId(roleId);
  }

  @Override
  @Transactional
  public void setRolePermissions(UUID roleId, Set<UUID> permissionIds) {
    Role role = requireRole(roleId);

    if (role.isSystem()) {
      throw SystemRoleModificationException.cannotChangePermissions(role.name());
    }

    rolePermissionRepository.saveRolePermissions(roleId, List.copyOf(permissionIds));
    eventPublisher.publishPermissionChanged(roleId.toString(), role.name(), "permissions_updated");
  }

  private Role requireRole(UUID id) {
    return roleRepository.findById(id).orElseThrow(() -> RoleNotFoundException.byId(id));
  }
}
