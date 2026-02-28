package com.gymapi.auth.adapter.out.persistence.repository;

import com.gymapi.auth.adapter.out.persistence.entity.PermissionEntity;
import com.gymapi.auth.adapter.out.persistence.entity.RoleEntity;
import com.gymapi.auth.adapter.out.persistence.entity.RolePermissionEntity;
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
class RolePermissionRepositoryAdapterTest {

    @Mock
    private RolePermissionJpaRepository rolePermissionJpaRepository;

    @Mock
    private RoleJpaRepository roleJpaRepository;

    @Mock
    private PermissionJpaRepository permissionJpaRepository;

    @InjectMocks
    private RolePermissionRepositoryAdapter adapter;

    @Test
    void findPermissionsByRoleId_Success() {
        UUID roleId = UUID.randomUUID();
        RolePermissionEntity rp = new RolePermissionEntity();
        PermissionEntity pe = new PermissionEntity();
        pe.setId(UUID.randomUUID());
        pe.setResource("res");
        pe.setAction("act");
        pe.setCreatedAt(OffsetDateTime.now());
        rp.setPermission(pe);

        given(rolePermissionJpaRepository.findByRoleId(roleId)).willReturn(List.of(rp));

        List<Permission> result = adapter.findPermissionsByRoleId(roleId);

        assertEquals(1, result.size());
        assertEquals(pe.getId(), result.get(0).id());
    }

    @Test
    void saveRolePermissions_Success() {
        UUID roleId = UUID.randomUUID();
        UUID permissionId = UUID.randomUUID();
        RoleEntity role = new RoleEntity();
        PermissionEntity permission = new PermissionEntity();

        given(roleJpaRepository.findById(roleId)).willReturn(Optional.of(role));
        given(permissionJpaRepository.findById(permissionId)).willReturn(Optional.of(permission));

        adapter.saveRolePermissions(roleId, List.of(permissionId));

        verify(rolePermissionJpaRepository).deleteByRoleId(roleId);
        verify(rolePermissionJpaRepository).saveAll(anyList());
    }

    @Test
    void deleteAllByRoleId_Success() {
        UUID roleId = UUID.randomUUID();
        adapter.deleteAllByRoleId(roleId);
        verify(rolePermissionJpaRepository).deleteByRoleId(roleId);
    }
}
