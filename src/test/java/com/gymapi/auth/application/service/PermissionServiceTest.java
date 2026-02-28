package com.gymapi.auth.application.service;

import com.gymapi.auth.application.port.out.PermissionRepository;
import com.gymapi.auth.domain.exception.DuplicatePermissionException;
import com.gymapi.auth.domain.exception.PermissionNotFoundException;
import com.gymapi.auth.domain.model.Permission;
import org.junit.jupiter.api.BeforeEach;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PermissionServiceTest {

    @Mock
    private PermissionRepository permissionRepository;

    @InjectMocks
    private PermissionService permissionService;

    private Permission testPermission;
    private UUID permissionId;

    @BeforeEach
    void setUp() {
        permissionId = UUID.randomUUID();
        testPermission = new Permission(
                permissionId,
                "booking",
                "read",
                "Read bookings",
                OffsetDateTime.now()
        );
    }

    @Test
    void createPermission_Success() {
        when(permissionRepository.existsByResourceAndAction("booking", "create")).thenReturn(false);
        when(permissionRepository.save(any(Permission.class))).thenReturn(testPermission);

        Permission result = permissionService.createPermission("booking", "create", "Create bookings");

        assertNotNull(result);
        assertEquals("booking", result.resource());
    }

    @Test
    void createPermission_Duplicate_ThrowsException() {
        when(permissionRepository.existsByResourceAndAction("booking", "read")).thenReturn(true);

        assertThrows(DuplicatePermissionException.class,
                () -> permissionService.createPermission("booking", "read", "Read bookings"));
    }

    @Test
    void getPermissionById_Success() {
        when(permissionRepository.findById(permissionId)).thenReturn(Optional.of(testPermission));

        Permission result = permissionService.getPermissionById(permissionId);

        assertNotNull(result);
        assertEquals(permissionId, result.id());
    }

    @Test
    void getPermissionById_NotFound_ThrowsException() {
        when(permissionRepository.findById(permissionId)).thenReturn(Optional.empty());

        assertThrows(PermissionNotFoundException.class,
                () -> permissionService.getPermissionById(permissionId));
    }

    @Test
    void getAllPermissions_Success() {
        when(permissionRepository.findAll()).thenReturn(List.of(testPermission));

        List<Permission> result = permissionService.getAllPermissions();

        assertEquals(1, result.size());
    }

    @Test
    void updatePermission_Success() {
        when(permissionRepository.findById(permissionId)).thenReturn(Optional.of(testPermission));
        when(permissionRepository.save(any(Permission.class))).thenReturn(testPermission);

        Permission result = permissionService.updatePermission(permissionId, "booking", "update", "Update bookings");

        assertNotNull(result);
    }

    @Test
    void deletePermission_Success() {
        when(permissionRepository.findById(permissionId)).thenReturn(Optional.of(testPermission));

        permissionService.deletePermission(permissionId);

        verify(permissionRepository).deleteById(permissionId);
    }
}
