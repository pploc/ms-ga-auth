package com.gymapi.auth.adapter.out.persistence.repository;

import com.gymapi.auth.adapter.out.persistence.entity.RolePermissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RolePermissionJpaRepository extends JpaRepository<RolePermissionEntity, UUID> {

    @Query("SELECT rp FROM RolePermissionEntity rp WHERE rp.role.id = :roleId")
    List<RolePermissionEntity> findByRoleId(UUID roleId);

    @Query("SELECT rp FROM RolePermissionEntity rp WHERE rp.permission.id = :permissionId")
    List<RolePermissionEntity> findByPermissionId(UUID permissionId);

    void deleteByRoleId(UUID roleId);
    void deleteByPermissionId(UUID permissionId);
}
