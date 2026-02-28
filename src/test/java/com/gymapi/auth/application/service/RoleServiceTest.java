package com.gymapi.auth.application.service;

import com.gymapi.auth.application.port.out.RolePermissionRepository;
import com.gymapi.auth.application.port.out.RoleRepository;
import com.gymapi.auth.domain.exception.DuplicateRoleException;
import com.gymapi.auth.domain.exception.RoleNotFoundException;
import com.gymapi.auth.domain.exception.SystemRoleDeletionException;
import com.gymapi.auth.domain.model.Permission;
import com.gymapi.auth.domain.model.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoleServiceTest {

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private RolePermissionRepository rolePermissionRepository;

    @InjectMocks
    private RoleService roleService;

    private Role testRole;
    private UUID roleId;

    @BeforeEach
    void setUp() {
        roleId = UUID.randomUUID();
        testRole = new Role(
                roleId,
                "TEST_ROLE",
                "Test role description",
                false,
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );
    }

    @Test
    void createRole_Success() {
        when(roleRepository.existsByName("NEW_ROLE")).thenReturn(false);
        when(roleRepository.save(any(Role.class))).thenReturn(testRole);

        Role result = roleService.createRole("NEW_ROLE", "Description", false);

        assertNotNull(result);
        assertEquals("TEST_ROLE", result.name());
        verify(roleRepository).save(any(Role.class));
    }

    @Test
    void createRole_DuplicateName_ThrowsException() {
        when(roleRepository.existsByName("EXISTING_ROLE")).thenReturn(true);

        assertThrows(DuplicateRoleException.class, 
                () -> roleService.createRole("EXISTING_ROLE", "Description", false));
    }

    @Test
    void getRoleById_Success() {
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(testRole));

        Role result = roleService.getRoleById(roleId);

        assertNotNull(result);
        assertEquals(roleId, result.id());
    }

    @Test
    void getRoleById_NotFound_ThrowsException() {
        when(roleRepository.findById(roleId)).thenReturn(Optional.empty());

        assertThrows(RoleNotFoundException.class, 
                () -> roleService.getRoleById(roleId));
    }

    @Test
    void getAllRoles_Success() {
        List<Role> roles = List.of(testRole);
        when(roleRepository.findAll()).thenReturn(roles);

        List<Role> result = roleService.getAllRoles();

        assertEquals(1, result.size());
    }

    @Test
    void updateRole_Success() {
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(testRole));
        when(roleRepository.save(any(Role.class))).thenReturn(testRole);

        Role result = roleService.updateRole(roleId, "UPDATED_ROLE", "New description");

        assertNotNull(result);
    }

    @Test
    void updateRole_SystemRole_ThrowsException() {
        Role systemRole = new Role(
                roleId,
                "SYSTEM_ROLE",
                "System role",
                true,
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(systemRole));

        assertThrows(SystemRoleDeletionException.class, 
                () -> roleService.updateRole(roleId, "NAME", "desc"));
    }

    @Test
    void deleteRole_Success() {
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(testRole));

        roleService.deleteRole(roleId);

        verify(roleRepository).deleteById(roleId);
    }

    @Test
    void deleteRole_SystemRole_ThrowsException() {
        Role systemRole = new Role(
                roleId,
                "SYSTEM_ROLE",
                "System role",
                true,
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(systemRole));

        assertThrows(SystemRoleDeletionException.class, 
                () -> roleService.deleteRole(roleId));
    }

    @Test
    void getRolePermissions_Success() {
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(testRole));
        List<Permission> permissions = new ArrayList<>();
        when(rolePermissionRepository.findPermissionsByRoleId(roleId)).thenReturn(permissions);

        List<Permission> result = roleService.getRolePermissions(roleId);

        assertNotNull(result);
    }

    @Test
    void setRolePermissions_Success() {
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(testRole));

        UUID permId = UUID.randomUUID();
        roleService.setRolePermissions(roleId, java.util.Set.of(permId));

        verify(rolePermissionRepository).saveRolePermissions(eq(roleId), anyList());
    }

    @Test
    void setRolePermissions_SystemRole_ThrowsException() {
        Role systemRole = new Role(
                roleId,
                "SYSTEM_ROLE",
                "System role",
                true,
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(systemRole));

        assertThrows(SystemRoleDeletionException.class, 
                () -> roleService.setRolePermissions(roleId, java.util.Set.of()));
    }
}
