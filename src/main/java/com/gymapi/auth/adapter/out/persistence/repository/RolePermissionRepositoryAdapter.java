package com.gymapi.auth.adapter.out.persistence.repository;

import com.gymapi.auth.adapter.out.persistence.entity.PermissionEntity;
import com.gymapi.auth.adapter.out.persistence.entity.RoleEntity;
import com.gymapi.auth.adapter.out.persistence.entity.RolePermissionEntity;
import com.gymapi.auth.application.port.out.RolePermissionRepository;
import com.gymapi.auth.domain.exception.PermissionNotFoundException;
import com.gymapi.auth.domain.exception.RoleNotFoundException;
import com.gymapi.auth.domain.model.Permission;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
public class RolePermissionRepositoryAdapter implements RolePermissionRepository {

  private final RolePermissionJpaRepository rolePermissionJpaRepository;
  private final RoleJpaRepository roleJpaRepository;
  private final PermissionJpaRepository permissionJpaRepository;

  @Override
  public List<Permission> findPermissionsByRoleId(UUID roleId) {
    List<RolePermissionEntity> rolePermissions = rolePermissionJpaRepository.findByRole_Id(roleId);
    List<Permission> permissions = new ArrayList<>();
    for (RolePermissionEntity rp : rolePermissions) {
      PermissionEntity pe = rp.getPermission();
      permissions.add(
          Permission.builder()
              .id(pe.getId())
              .resource(pe.getResource())
              .action(pe.getAction())
              .description(pe.getDescription())
              .createdAt(pe.getCreatedAt())
              .build());
    }
    return permissions;
  }

  @Override
  @Transactional
  public void saveRolePermissions(UUID roleId, List<UUID> permissionIds) {
    rolePermissionJpaRepository.deleteByRole_Id(roleId);

    RoleEntity role =
        roleJpaRepository.findById(roleId).orElseThrow(() -> RoleNotFoundException.byId(roleId));

    List<RolePermissionEntity> newPermissions = new ArrayList<>();
    for (UUID permissionId : permissionIds) {
      // A bare orElseThrow here would surface an unknown permission id as a 500;
      // the domain exception makes it the documented 404.
      PermissionEntity permission =
          permissionJpaRepository
              .findById(permissionId)
              .orElseThrow(() -> PermissionNotFoundException.byId(permissionId));
      newPermissions.add(
          RolePermissionEntity.builder()
              .role(role)
              .permission(permission)
              .assignedAt(OffsetDateTime.now())
              .build());
    }

    rolePermissionJpaRepository.saveAll(newPermissions);
  }

  @Override
  @Transactional
  public void deleteAllByRoleId(UUID roleId) {
    rolePermissionJpaRepository.deleteByRole_Id(roleId);
  }
}
