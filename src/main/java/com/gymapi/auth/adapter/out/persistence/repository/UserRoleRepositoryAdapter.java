package com.gymapi.auth.adapter.out.persistence.repository;

import com.gymapi.auth.adapter.out.persistence.entity.UserRoleEntity;
import com.gymapi.auth.adapter.out.persistence.mapper.AuthPersistenceMapper;
import com.gymapi.auth.application.port.out.UserRoleRepository;
import com.gymapi.auth.domain.model.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class UserRoleRepositoryAdapter implements UserRoleRepository {

    private final UserRoleJpaRepository userRoleJpaRepository;
    private final AuthPersistenceMapper mapper;

    @Override
    public UserRole save(UserRole userRole) {
        UserRoleEntity entity = mapper.toUserRoleEntity(userRole);
        UserRoleEntity saved = userRoleJpaRepository.save(entity);
        return mapper.toUserRole(saved);
    }

    @Override
    public Optional<UserRole> findById(UUID id) {
        return userRoleJpaRepository.findById(id).map(mapper::toUserRole);
    }

    @Override
    public List<UserRole> findByUserId(UUID userId) {
        return mapper.toUserRoleList(userRoleJpaRepository.findByUserId(userId));
    }

    @Override
    public List<UserRole> findByRoleId(UUID roleId) {
        return mapper.toUserRoleList(userRoleJpaRepository.findByRoleId(roleId));
    }

    @Override
    public Optional<UserRole> findByUserIdAndRoleId(UUID userId, UUID roleId) {
        return userRoleJpaRepository.findByUserIdAndRoleId(userId, roleId).map(mapper::toUserRole);
    }

    @Override
    public boolean existsByUserIdAndRoleId(UUID userId, UUID roleId) {
        return userRoleJpaRepository.existsByUserIdAndRoleId(userId, roleId);
    }

    @Override
    public void deleteByUserIdAndRoleId(UUID userId, UUID roleId) {
        userRoleJpaRepository.deleteByUserIdAndRoleId(userId, roleId);
    }

    @Override
    public void deleteByUserId(UUID userId) {
        userRoleJpaRepository.deleteByUserId(userId);
    }

    @Override
    public void deleteByRoleId(UUID roleId) {
        userRoleJpaRepository.deleteByRoleId(roleId);
    }
}
