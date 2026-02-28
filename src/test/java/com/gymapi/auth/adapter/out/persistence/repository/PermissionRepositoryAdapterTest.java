package com.gymapi.auth.adapter.out.persistence.repository;

import com.gymapi.auth.adapter.out.persistence.entity.PermissionEntity;
import com.gymapi.auth.adapter.out.persistence.mapper.AuthPersistenceMapper;
import com.gymapi.auth.domain.model.Permission;
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
class PermissionRepositoryAdapterTest {

    @Mock
    private PermissionJpaRepository permissionJpaRepository;

    @Mock
    private AuthPersistenceMapper mapper;

    @InjectMocks
    private PermissionRepositoryAdapter adapter;

    @Test
    void save_Success() {
        Permission permission = new Permission(UUID.randomUUID(), "res", "act", "desc", OffsetDateTime.now());
        PermissionEntity entity = new PermissionEntity();

        given(mapper.toPermissionEntity(permission)).willReturn(entity);
        given(permissionJpaRepository.save(entity)).willReturn(entity);
        given(mapper.toPermission(entity)).willReturn(permission);

        Permission result = adapter.save(permission);

        assertNotNull(result);
        verify(permissionJpaRepository).save(entity);
    }

    @Test
    void findByResourceAndAction_Success() {
        PermissionEntity entity = new PermissionEntity();
        Permission permission = new Permission(UUID.randomUUID(), "res", "act", "desc", OffsetDateTime.now());

        given(permissionJpaRepository.findByResourceAndAction("res", "act")).willReturn(Optional.of(entity));
        given(mapper.toPermission(entity)).willReturn(permission);

        Optional<Permission> result = adapter.findByResourceAndAction("res", "act");

        assertTrue(result.isPresent());
        assertEquals("res", result.get().resource());
    }

    @Test
    void findById_Success() {
        UUID id = UUID.randomUUID();
        PermissionEntity entity = new PermissionEntity();
        Permission permission = new Permission(id, "res", "act", "desc", OffsetDateTime.now());

        given(permissionJpaRepository.findById(id)).willReturn(Optional.of(entity));
        given(mapper.toPermission(entity)).willReturn(permission);

        Optional<Permission> result = adapter.findById(id);

        assertTrue(result.isPresent());
        assertEquals(id, result.get().id());
    }

    @Test
    void existsByResourceAndAction_True() {
        given(permissionJpaRepository.existsByResourceAndAction("res", "act")).willReturn(true);
        assertTrue(adapter.existsByResourceAndAction("res", "act"));
    }

    @Test
    void deleteById_Success() {
        UUID id = UUID.randomUUID();
        adapter.deleteById(id);
        verify(permissionJpaRepository).deleteById(id);
    }

    @Test
    void findAll_Success() {
        PermissionEntity entity = new PermissionEntity();
        Permission permission = new Permission(UUID.randomUUID(), "res", "act", "desc", OffsetDateTime.now());

        given(permissionJpaRepository.findAll()).willReturn(List.of(entity));
        given(mapper.toPermissionList(anyList())).willReturn(List.of(permission));

        List<Permission> result = adapter.findAll();

        assertEquals(1, result.size());
    }
}
