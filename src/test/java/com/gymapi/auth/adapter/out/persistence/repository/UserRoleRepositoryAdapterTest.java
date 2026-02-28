package com.gymapi.auth.adapter.out.persistence.repository;

import com.gymapi.auth.adapter.out.persistence.entity.UserRoleEntity;
import com.gymapi.auth.adapter.out.persistence.mapper.AuthPersistenceMapper;
import com.gymapi.auth.domain.model.UserRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserRoleRepositoryAdapterTest {

    @Mock
    private UserRoleJpaRepository userRoleJpaRepository;

    @Mock
    private AuthPersistenceMapper mapper;

    @InjectMocks
    private UserRoleRepositoryAdapter adapter;

    @Test
    void save_Success() {
        UserRole userRole = new UserRole(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                OffsetDateTime.now());
        UserRoleEntity entity = new UserRoleEntity();

        given(mapper.toUserRoleEntity(userRole)).willReturn(entity);
        given(userRoleJpaRepository.save(entity)).willReturn(entity);
        given(mapper.toUserRole(entity)).willReturn(userRole);

        UserRole result = adapter.save(userRole);

        assertNotNull(result);
        verify(userRoleJpaRepository).save(entity);
    }

    @Test
    void findByUserId_Success() {
        UUID userId = UUID.randomUUID();
        UserRoleEntity entity = new UserRoleEntity();
        UserRole userRole = new UserRole(UUID.randomUUID(), userId, UUID.randomUUID(), UUID.randomUUID(),
                OffsetDateTime.now());

        given(userRoleJpaRepository.findByUserId(userId)).willReturn(List.of(entity));
        given(mapper.toUserRoleList(anyList())).willReturn(List.of(userRole));

        List<UserRole> result = adapter.findByUserId(userId);

        assertEquals(1, result.size());
    }

    @Test
    void findByUserIdAndRoleId_Success() {
        UUID userId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        UserRoleEntity entity = new UserRoleEntity();
        UserRole userRole = new UserRole(UUID.randomUUID(), userId, roleId, UUID.randomUUID(), OffsetDateTime.now());

        given(userRoleJpaRepository.findByUserIdAndRole_Id(userId, roleId)).willReturn(Optional.of(entity));
        given(mapper.toUserRole(entity)).willReturn(userRole);

        Optional<UserRole> result = adapter.findByUserIdAndRoleId(userId, roleId);

        assertTrue(result.isPresent());
    }

    @Test
    void findById_Success() {
        UUID id = UUID.randomUUID();
        UserRoleEntity entity = new UserRoleEntity();
        UserRole userRole = new UserRole(id, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                OffsetDateTime.now());

        given(userRoleJpaRepository.findById(id)).willReturn(Optional.of(entity));
        given(mapper.toUserRole(entity)).willReturn(userRole);

        Optional<UserRole> result = adapter.findById(id);

        assertTrue(result.isPresent());
        assertEquals(id, result.get().id());
    }

    @Test
    void findByRoleId_Success() {
        UUID roleId = UUID.randomUUID();
        given(userRoleJpaRepository.findByRoleId(roleId)).willReturn(List.of());
        given(mapper.toUserRoleList(anyList())).willReturn(List.of());

        List<UserRole> result = adapter.findByRoleId(roleId);
        assertNotNull(result);
    }

    @Test
    void existsByUserIdAndRoleId_True() {
        UUID userId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        given(userRoleJpaRepository.existsByUserIdAndRole_Id(userId, roleId)).willReturn(true);
        assertTrue(adapter.existsByUserIdAndRoleId(userId, roleId));
    }

    @Test
    void deleteByUserIdAndRoleId_Success() {
        UUID userId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        adapter.deleteByUserIdAndRoleId(userId, roleId);
        verify(userRoleJpaRepository).deleteByUserIdAndRole_Id(userId, roleId);
    }

    @Test
    void deleteByUserId_Success() {
        UUID userId = UUID.randomUUID();
        adapter.deleteByUserId(userId);
        verify(userRoleJpaRepository).deleteByUserId(userId);
    }

    @Test
    void deleteByRoleId_Success() {
        UUID roleId = UUID.randomUUID();
        adapter.deleteByRoleId(roleId);
        verify(userRoleJpaRepository).deleteByRole_Id(roleId);
    }
}
