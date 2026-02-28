package com.gymapi.auth.adapter.out.persistence;

import com.gymapi.auth.adapter.out.persistence.entity.PermissionEntity;
import com.gymapi.auth.adapter.out.persistence.entity.RoleEntity;
import com.gymapi.auth.adapter.out.persistence.entity.UserRoleEntity;
import com.gymapi.auth.adapter.out.persistence.mapper.AuthPersistenceMapper;
import com.gymapi.auth.adapter.out.persistence.repository.UserRoleJpaRepository;
import com.gymapi.auth.application.port.out.UserRoleRepository;
import com.gymapi.auth.domain.model.Role;
import com.gymapi.auth.domain.model.RolesWithPermissions;
import com.gymapi.auth.domain.model.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class UserRoleRepositoryAdapter implements UserRoleRepository {

    private final UserRoleJpaRepository userRoleJpaRepository;
    private final AuthPersistenceMapper mapper;

    @Override
    public UserRole save(UserRole userRole) {
        UserRoleEntity entity = mapper.mapToEntity(userRole);
        UserRoleEntity savedEntity = userRoleJpaRepository.save(entity);
        return mapper.mapToDomain(savedEntity);
    }

    @Override
    public void deleteByUserIdAndRoleId(UUID userId, UUID roleId) {
        userRoleJpaRepository.deleteByUserIdAndRoleId(userId, roleId);
    }

    @Override
    public List<Role> findRolesByUserId(UUID userId) {
        return userRoleJpaRepository.findByUserId(userId).stream()
                .map(UserRoleEntity::getRole)
                .map(mapper::mapToDomain)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsByUserIdAndRoleId(UUID userId, UUID roleId) {
        return userRoleJpaRepository.existsByUserIdAndRoleId(userId, roleId);
    }

    @Override
    public RolesWithPermissions findRolesWithPermissionsByUserId(UUID userId) {
        List<RoleEntity> roleEntities = userRoleJpaRepository.findByUserId(userId).stream()
                .map(UserRoleEntity::getRole)
                .toList();

        List<String> roleNames = roleEntities.stream()
                .map(RoleEntity::getName)
                .collect(Collectors.toList());

        Set<String> permissionNames = roleEntities.stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(p -> p.getResource() + ":" + p.getAction())
                .collect(Collectors.toSet());

        return RolesWithPermissions.builder()
                .userId(userId)
                .roles(roleNames)
                .permissions(permissionNames.stream().toList())
                .build();
    }
}
