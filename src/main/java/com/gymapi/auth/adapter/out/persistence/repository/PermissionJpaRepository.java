package com.gymapi.auth.adapter.out.persistence.repository;

import com.gymapi.auth.adapter.out.persistence.entity.PermissionEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PermissionJpaRepository extends JpaRepository<PermissionEntity, UUID> {
  Optional<PermissionEntity> findByResourceAndAction(String resource, String action);

  boolean existsByResourceAndAction(String resource, String action);
}
