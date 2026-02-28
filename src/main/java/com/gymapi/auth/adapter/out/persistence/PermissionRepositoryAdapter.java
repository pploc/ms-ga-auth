package com.gymapi.auth.adapter.out.persistence;

import com.gymapi.auth.adapter.out.persistence.entity.PermissionEntity;
import com.gymapi.auth.adapter.out.persistence.mapper.AuthPersistenceMapper;
import com.gymapi.auth.adapter.out.persistence.repository.PermissionJpaRepository;
import com.gymapi.auth.application.port.out.PermissionRepository;
import com.gymapi.auth.domain.model.Permission;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class PermissionRepositoryAdapter implements PermissionRepository {

    private final PermissionJpaRepository permissionJpaRepository;
    private final AuthPersistenceMapper mapper;

    @Override
    public Permission save(Permission permission) {
        PermissionEntity entity = mapper.mapToEntity(permission);
        PermissionEntity savedEntity = permissionJpaRepository.save(entity);
        return mapper.mapToDomain(savedEntity);
    }

    @Override
    public Optional<Permission> findById(UUID id) {
        return permissionJpaRepository.findById(id).map(mapper::mapToDomain);
    }

    @Override
    public List<Permission> findAll() {
        return permissionJpaRepository.findAll().stream()
                .map(mapper::mapToDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Permission> findAllByIds(Set<UUID> ids) {
        return permissionJpaRepository.findAllById(ids).stream()
                .map(mapper::mapToDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(UUID id) {
        permissionJpaRepository.deleteById(id);
    }

    @Override
    public boolean existsByResourceAndAction(String resource, String action) {
        return permissionJpaRepository.existsByResourceAndAction(resource, action);
    }
}
