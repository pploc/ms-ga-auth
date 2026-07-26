package com.gymapi.auth.adapter.out.persistence.repository;

import com.gymapi.auth.adapter.out.persistence.entity.RoleEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RoleJpaRepository extends JpaRepository<RoleEntity, UUID> {
  Optional<RoleEntity> findByName(String name);

  boolean existsByName(String name);
}
