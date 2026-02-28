package com.gymapi.auth.adapter.out.persistence.repository;

import com.gymapi.auth.adapter.out.persistence.entity.UserRoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRoleJpaRepository extends JpaRepository<UserRoleEntity, UUID> {

    @Query("SELECT ur FROM UserRoleEntity ur WHERE ur.userId = :userId")
    List<UserRoleEntity> findByUserId(UUID userId);

    @Query("SELECT ur FROM UserRoleEntity ur WHERE ur.role.id = :roleId")
    List<UserRoleEntity> findByRoleId(UUID roleId);

    Optional<UserRoleEntity> findByUserIdAndRole_Id(UUID userId, UUID roleId);

    boolean existsByUserIdAndRole_Id(UUID userId, UUID roleId);

    void deleteByUserIdAndRole_Id(UUID userId, UUID roleId);

    void deleteByUserId(UUID userId);

    void deleteByRole_Id(UUID roleId);
}
