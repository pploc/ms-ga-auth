package com.gymapi.auth.application.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.gymapi.auth.application.port.out.PermissionRepository;
import com.gymapi.auth.domain.exception.DuplicatePermissionException;
import com.gymapi.auth.domain.exception.PermissionNotFoundException;
import com.gymapi.auth.domain.model.Permission;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PermissionServiceTest {

  @Mock private PermissionRepository permissionRepository;

  @InjectMocks private PermissionService permissionService;

  private Permission testPermission;
  private UUID permissionId;

  @BeforeEach
  void setUp() {
    permissionId = UUID.randomUUID();
    testPermission =
        new Permission(permissionId, "booking", "read", "Read bookings", OffsetDateTime.now());
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

    assertThrows(
        DuplicatePermissionException.class,
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

    assertThrows(
        PermissionNotFoundException.class, () -> permissionService.getPermissionById(permissionId));
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

    Permission result =
        permissionService.updatePermission(permissionId, "booking", "update", "Update bookings");

    assertNotNull(result);
  }

  @Test
  void deletePermission_Success() {
    when(permissionRepository.findById(permissionId)).thenReturn(Optional.of(testPermission));

    permissionService.deletePermission(permissionId);

    verify(permissionRepository).deleteById(permissionId);
  }

  @Test
  void deletePermission_NotFound_ThrowsException() {
    when(permissionRepository.findById(permissionId)).thenReturn(Optional.empty());

    assertThrows(
        PermissionNotFoundException.class, () -> permissionService.deletePermission(permissionId));
  }

  @Test
  void updatePermission_NotFound_ThrowsException() {
    when(permissionRepository.findById(permissionId)).thenReturn(Optional.empty());

    assertThrows(
        PermissionNotFoundException.class,
        () -> permissionService.updatePermission(permissionId, "res", "act", "desc"));
  }

  @Test
  void updatePermission_Duplicate_ThrowsException() {
    when(permissionRepository.findById(permissionId)).thenReturn(Optional.of(testPermission));
    when(permissionRepository.existsByResourceAndAction("booking", "create")).thenReturn(true);

    assertThrows(
        DuplicatePermissionException.class,
        () -> permissionService.updatePermission(permissionId, "booking", "create", "desc"));
  }

  @Test
  void updatePermission_UnchangedPair_SkipsTheDuplicateCheck() {
    when(permissionRepository.findById(permissionId)).thenReturn(Optional.of(testPermission));
    when(permissionRepository.save(any(Permission.class))).thenReturn(testPermission);

    permissionService.updatePermission(permissionId, "booking", "read", "New description");

    // Re-saving a permission under its own resource:action must not trip the uniqueness guard.
    verify(permissionRepository, never()).existsByResourceAndAction(anyString(), anyString());
  }

  @Test
  void updatePermission_ChangedResourceOnly_ChecksForADuplicate() {
    when(permissionRepository.findById(permissionId)).thenReturn(Optional.of(testPermission));
    when(permissionRepository.existsByResourceAndAction("exercise", "read")).thenReturn(false);
    when(permissionRepository.save(any(Permission.class))).thenReturn(testPermission);

    permissionService.updatePermission(permissionId, "exercise", "read", "desc");

    verify(permissionRepository).existsByResourceAndAction("exercise", "read");
  }

  @Test
  void updatePermission_ChangedActionOnly_ChecksForADuplicate() {
    when(permissionRepository.findById(permissionId)).thenReturn(Optional.of(testPermission));
    when(permissionRepository.existsByResourceAndAction("booking", "delete")).thenReturn(false);
    when(permissionRepository.save(any(Permission.class))).thenReturn(testPermission);

    permissionService.updatePermission(permissionId, "booking", "delete", "desc");

    verify(permissionRepository).existsByResourceAndAction("booking", "delete");
  }

  @Test
  void getPermissionByResourceAndAction_Success() {
    when(permissionRepository.findByResourceAndAction("booking", "read"))
        .thenReturn(Optional.of(testPermission));

    Permission result = permissionService.getPermissionByResourceAndAction("booking", "read");

    assertNotNull(result);
    assertEquals(permissionId, result.id());
  }

  @Test
  void getPermissionByResourceAndAction_NotFound_ThrowsException() {
    when(permissionRepository.findByResourceAndAction("booking", "none"))
        .thenReturn(Optional.empty());

    assertThrows(
        PermissionNotFoundException.class,
        () -> permissionService.getPermissionByResourceAndAction("booking", "none"));
  }
}
