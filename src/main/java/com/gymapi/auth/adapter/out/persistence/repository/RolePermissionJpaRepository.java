package com.gymapi.auth.adapter.out.persistence.repository;

import com.gymapi.auth.adapter.out.persistence.entity.RolePermissionEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Query methods are derived from their names. The underscore marks the traversal explicitly: {@code
 * findByRole_Id} reads the {@code role} association's {@code id}, which is what the previous
 * hand-written JPQL said in more characters.
 */
@Repository
public interface RolePermissionJpaRepository extends JpaRepository<RolePermissionEntity, UUID> {

  /** Callers map straight to permissions, so the association is fetched rather than left lazy. */
  @EntityGraph(attributePaths = "permission")
  List<RolePermissionEntity> findByRole_Id(UUID roleId);

  @EntityGraph(attributePaths = "permission")
  List<RolePermissionEntity> findByPermission_Id(UUID permissionId);

  void deleteByRole_Id(UUID roleId);

  void deleteByPermission_Id(UUID permissionId);
}
