package com.gymapi.auth.application.port.out;

import com.gymapi.auth.domain.model.UserRole;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRoleRepository {
    UserRole save(UserRole userRole);
    Optional<UserRole> findById(UUID id);
    List<UserRole> findByUserId(UUID userId);
    List<UserRole> findByRoleId(UUID roleId);
    Optional<UserRole> findByUserIdAndRoleId(UUID userId, UUID roleId);
    boolean existsByUserIdAndRoleId(UUID userId, UUID roleId);
    void deleteByUserIdAndRoleId(UUID userId, UUID roleId);
    void deleteByUserId(UUID userId);
    void deleteByRoleId(UUID roleId);
}
