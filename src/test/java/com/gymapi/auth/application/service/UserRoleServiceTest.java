package com.gymapi.auth.application.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.gymapi.auth.application.port.out.RolePermissionRepository;
import com.gymapi.auth.application.port.out.RoleRepository;
import com.gymapi.auth.application.port.out.UserRoleRepository;
import com.gymapi.auth.domain.exception.DuplicateUserRoleException;
import com.gymapi.auth.domain.exception.RoleNotFoundException;
import com.gymapi.auth.domain.exception.UserRoleNotFoundException;
import com.gymapi.auth.domain.model.Permission;
import com.gymapi.auth.domain.model.Role;
import com.gymapi.auth.domain.model.RolesWithPermissions;
import com.gymapi.auth.domain.model.UserRole;
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
class UserRoleServiceTest {

  @Mock private UserRoleRepository userRoleRepository;

  @Mock private RoleRepository roleRepository;

  @Mock private RolePermissionRepository rolePermissionRepository;

  @Mock private com.gymapi.auth.application.port.out.EventPublisher eventPublisher;

  @InjectMocks private UserRoleService userRoleService;

  private UUID userId;
  private UUID roleId;
  private UserRole testUserRole;
  private Role testRole;

  @BeforeEach
  void setUp() {
    userId = UUID.randomUUID();
    roleId = UUID.randomUUID();

    testRole =
        new Role(roleId, "MEMBER", "Member role", true, OffsetDateTime.now(), OffsetDateTime.now());

    testUserRole =
        new UserRole(
            UUID.randomUUID(), userId, roleId, "MEMBER", UUID.randomUUID(), OffsetDateTime.now());
  }

  @Test
  void assignRole_Success() {
    when(roleRepository.findById(roleId)).thenReturn(Optional.of(testRole));
    when(userRoleRepository.existsByUserIdAndRoleId(userId, roleId)).thenReturn(false);
    when(userRoleRepository.save(any(UserRole.class))).thenReturn(testUserRole);

    UserRole result = userRoleService.assignRole(userId, roleId, UUID.randomUUID());

    assertNotNull(result);
    assertEquals(userId, result.userId());
    assertEquals(roleId, result.roleId());
    verify(eventPublisher).publishRoleAssigned(anyString(), anyString(), anyString());
  }

  @Test
  void assignRole_AlreadyAssigned_ThrowsException() {
    when(roleRepository.findById(roleId)).thenReturn(Optional.of(testRole));
    when(userRoleRepository.existsByUserIdAndRoleId(userId, roleId)).thenReturn(true);

    assertThrows(
        DuplicateUserRoleException.class,
        () -> userRoleService.assignRole(userId, roleId, UUID.randomUUID()));
  }

  @Test
  void assignRole_UnknownRole_ThrowsException() {
    when(roleRepository.findById(roleId)).thenReturn(Optional.empty());

    assertThrows(
        RoleNotFoundException.class,
        () -> userRoleService.assignRole(userId, roleId, UUID.randomUUID()));

    verify(userRoleRepository, never()).save(any(UserRole.class));
    verifyNoInteractions(eventPublisher);
  }

  @Test
  void assignRole_NullAssignedBy_PublishesEventWithoutActor() {
    when(roleRepository.findById(roleId)).thenReturn(Optional.of(testRole));
    when(userRoleRepository.existsByUserIdAndRoleId(userId, roleId)).thenReturn(false);
    when(userRoleRepository.save(any(UserRole.class))).thenReturn(testUserRole);

    userRoleService.assignRole(userId, roleId, null);

    verify(eventPublisher).publishRoleAssigned(userId.toString(), roleId.toString(), null);
  }

  @Test
  void revokeRole_Success() {
    when(userRoleRepository.findByUserIdAndRoleId(userId, roleId))
        .thenReturn(Optional.of(testUserRole));

    userRoleService.revokeRole(userId, roleId);

    verify(userRoleRepository).deleteByUserIdAndRoleId(userId, roleId);
    verify(eventPublisher).publishRoleRevoked(anyString(), anyString());
  }

  @Test
  void revokeRole_NotAssigned_ThrowsException() {
    when(userRoleRepository.findByUserIdAndRoleId(userId, roleId)).thenReturn(Optional.empty());

    assertThrows(UserRoleNotFoundException.class, () -> userRoleService.revokeRole(userId, roleId));
  }

  @Test
  void getUserRoles_Success() {
    when(userRoleRepository.findByUserId(userId)).thenReturn(List.of(testUserRole));

    List<UserRole> result = userRoleService.getUserRoles(userId);

    assertEquals(1, result.size());
  }

  @Test
  void hasRole_True() {
    when(userRoleRepository.existsByUserIdAndRoleId(userId, roleId)).thenReturn(true);

    boolean result = userRoleService.hasRole(userId, roleId);

    assertTrue(result);
  }

  @Test
  void hasRole_False() {
    when(userRoleRepository.existsByUserIdAndRoleId(userId, roleId)).thenReturn(false);

    boolean result = userRoleService.hasRole(userId, roleId);

    assertFalse(result);
  }

  @Test
  void getUserRolesWithPermissions_Success() {
    Permission perm =
        new Permission(UUID.randomUUID(), "booking", "read", "Read", OffsetDateTime.now());

    when(userRoleRepository.findByUserId(userId)).thenReturn(List.of(testUserRole));
    when(roleRepository.findById(roleId)).thenReturn(Optional.of(testRole));
    when(rolePermissionRepository.findPermissionsByRoleId(roleId)).thenReturn(List.of(perm));

    RolesWithPermissions result = userRoleService.getUserRolesWithPermissions(userId);

    assertNotNull(result);
    assertEquals(userId, result.userId());
    assertTrue(result.roles().contains("MEMBER"));
    assertTrue(result.permissions().contains("booking:read"));
  }

  @Test
  void getUserRolesWithPermissions_SkipsOrphanedAssignment() {
    when(userRoleRepository.findByUserId(userId)).thenReturn(List.of(testUserRole));
    when(roleRepository.findById(roleId)).thenReturn(Optional.empty());

    RolesWithPermissions result = userRoleService.getUserRolesWithPermissions(userId);

    assertTrue(result.roles().isEmpty());
    assertTrue(result.permissions().isEmpty());
    verifyNoInteractions(rolePermissionRepository);
  }

  @Test
  void getUserRolesWithPermissions_NoAssignments_ReturnsEmptyLists() {
    when(userRoleRepository.findByUserId(userId)).thenReturn(List.of());

    RolesWithPermissions result = userRoleService.getUserRolesWithPermissions(userId);

    assertEquals(userId, result.userId());
    assertTrue(result.roles().isEmpty());
    assertTrue(result.permissions().isEmpty());
  }

  @Test
  void getRoleUsers_Success() {
    when(userRoleRepository.findByRoleId(roleId)).thenReturn(List.of(testUserRole));

    List<UserRole> result = userRoleService.getRoleUsers(roleId);

    assertEquals(1, result.size());
  }
}
