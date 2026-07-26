package com.gymapi.auth.application.port.out;

import com.gymapi.auth.domain.model.Permission;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PermissionRepository {
  Permission save(Permission permission);

  Optional<Permission> findById(UUID id);

  Optional<Permission> findByResourceAndAction(String resource, String action);

  List<Permission> findAll();

  boolean existsByResourceAndAction(String resource, String action);

  void deleteById(UUID id);
}
