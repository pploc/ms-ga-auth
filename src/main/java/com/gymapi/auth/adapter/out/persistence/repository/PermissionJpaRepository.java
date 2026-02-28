package com.gymapi.auth.adapter.out.persistence.repository;

import com.gymapi.auth.adapter.out.persistence.entity.PermissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PermissionJpaRepository extends JpaRepository<PermissionEntity, UUID> {
    boolean existsByResourceAndAction(String resource, String action);
}
