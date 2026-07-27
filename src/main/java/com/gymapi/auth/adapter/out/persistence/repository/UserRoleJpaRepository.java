package com.gymapi.auth.adapter.out.persistence.repository;

import com.gymapi.auth.adapter.out.persistence.entity.UserRoleEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * All queries are derived from their method names; Spring Data validates the property paths when
 * the context starts, so a renamed field fails the build rather than at runtime.
 *
 * <p>The role is fetched alongside the assignment because callers map it into a response carrying
 * {@code roleName} — leaving it lazy would cost one extra select per row.
 */
@Repository
public interface UserRoleJpaRepository extends JpaRepository<UserRoleEntity, UUID> {

  @EntityGraph(attributePaths = "role")
  List<UserRoleEntity> findByUserId(UUID userId);

  @EntityGraph(attributePaths = "role")
  List<UserRoleEntity> findByRole_Id(UUID roleId);

  @EntityGraph(attributePaths = "role")
  Optional<UserRoleEntity> findByUserIdAndRole_Id(UUID userId, UUID roleId);

  boolean existsByUserIdAndRole_Id(UUID userId, UUID roleId);

  void deleteByUserIdAndRole_Id(UUID userId, UUID roleId);

  void deleteByUserId(UUID userId);

  void deleteByRole_Id(UUID roleId);
}
