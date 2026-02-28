package com.gymapi.auth.adapter.out.persistence.repository;

import com.gymapi.auth.adapter.out.persistence.entity.PermissionEntity;
import com.gymapi.auth.adapter.out.persistence.mapper.AuthPersistenceMapper;
import com.gymapi.auth.application.port.out.PermissionRepository;
import com.gymapi.auth.domain.model.Permission;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class PermissionRepositoryAdapter implements PermissionRepository {

    private final PermissionJpaRepository permissionJpaRepository;
    private final AuthPersistenceMapper mapper;

    @Override
    public Permission save(Permission permission) {
        PermissionEntity entity = mapper.toPermissionEntity(permission);
        PermissionEntity saved = permissionJpaRepository.save(entity);
        return mapper.toPermission(saved);
    }

    @Override
    public Optional<Permission> findById(UUID id) {
        return permissionJpaRepository.findById(id).map(mapper::toPermission);
    }

    @Override
    public Optional<Permission> findByResourceAndAction(String resource, String action) {
        return permissionJpaRepository.findByResourceAndAction(resource, action).map(mapper::toPermission);
    }

    @Override
    public List<Permission> findAll() {
        return mapper.toPermissionList(permissionJpaRepository.findAll());
    }

    @Override
    public boolean existsByResourceAndAction(String resource, String action) {
        return permissionJpaRepository.existsByResourceAndAction(resource, action);
    }

    @Override
    public void deleteById(UUID id) {
        permissionJpaRepository.deleteById(id);
    }
}
