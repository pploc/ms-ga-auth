package com.gymapi.auth.application.port.out;

import com.gymapi.auth.domain.model.Permission;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface PermissionRepository {
    Permission save(Permission permission);

    Optional<Permission> findById(UUID id);

    List<Permission> findAll();

    List<Permission> findAllByIds(Set<UUID> ids);

    void deleteById(UUID id);

    boolean existsByResourceAndAction(String resource, String action);
}
