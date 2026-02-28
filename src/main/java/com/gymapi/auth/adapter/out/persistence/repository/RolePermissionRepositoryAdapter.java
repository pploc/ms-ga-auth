package com.gymapi.auth.adapter.out.persistence.repository;

import com.gymapi.auth.adapter.out.persistence.entity.PermissionEntity;
import com.gymapi.auth.adapter.out.persistence.entity.RoleEntity;
import com.gymapi.auth.adapter.out.persistence.entity.RolePermissionEntity;
import com.gymapi.auth.application.port.out.RolePermissionRepository;
import com.gymapi.auth.domain.model.Permission;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class RolePermissionRepositoryAdapter implements RolePermissionRepository {

    private final RolePermissionJpaRepository rolePermissionJpaRepository;
    private final RoleJpaRepository roleJpaRepository;
    private final PermissionJpaRepository permissionJpaRepository;

    @Override
    public List<Permission> findPermissionsByRoleId(UUID roleId) {
        List<RolePermissionEntity> rolePermissions = rolePermissionJpaRepository.findByRoleId(roleId);
        List<Permission> permissions = new ArrayList<>();
        for (RolePermissionEntity rp : rolePermissions) {
            PermissionEntity pe = rp.getPermission();
            Permission p = new Permission(
                    pe.getId(),
                    pe.getResource(),
                    pe.getAction(),
                    pe.getDescription(),
                    pe.getCreatedAt()
            );
            permissions.add(p);
        }
        return permissions;
    }

    @Override
    @Transactional
    public void saveRolePermissions(UUID roleId, List<UUID> permissionIds) {
        rolePermissionJpaRepository.deleteByRoleId(roleId);
        
        RoleEntity role = roleJpaRepository.findById(roleId).orElseThrow();
        
        List<RolePermissionEntity> newPermissions = new ArrayList<>();
        for (UUID permissionId : permissionIds) {
            PermissionEntity permission = permissionJpaRepository.findById(permissionId).orElseThrow();
            RolePermissionEntity rp = new RolePermissionEntity();
            rp.setRole(role);
            rp.setPermission(permission);
            rp.setAssignedAt(OffsetDateTime.now());
            newPermissions.add(rp);
        }
        
        rolePermissionJpaRepository.saveAll(newPermissions);
    }

    @Override
    @Transactional
    public void deleteAllByRoleId(UUID roleId) {
        rolePermissionJpaRepository.deleteByRoleId(roleId);
    }
}
