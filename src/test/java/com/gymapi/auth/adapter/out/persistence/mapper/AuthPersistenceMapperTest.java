package com.gymapi.auth.adapter.out.persistence.mapper;

import static org.junit.jupiter.api.Assertions.*;

import com.gymapi.auth.adapter.out.persistence.entity.*;
import com.gymapi.auth.domain.model.*;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class AuthPersistenceMapperTest {

  private final AuthPersistenceMapper mapper = Mappers.getMapper(AuthPersistenceMapper.class);

  @Test
  void toRole() {
    RoleEntity entity = new RoleEntity();
    UUID id = UUID.randomUUID();
    entity.setId(id);
    entity.setName("USER");
    entity.setDescription("Regular user");
    entity.setSystem(false);
    entity.setCreatedAt(OffsetDateTime.now());
    entity.setUpdatedAt(OffsetDateTime.now());

    Role role = mapper.toRole(entity);

    assertNotNull(role);
    assertEquals(id, role.id());
    assertEquals("USER", role.name());
    assertFalse(role.isSystem());
  }

  @Test
  void toPermission() {
    PermissionEntity entity = new PermissionEntity();
    UUID id = UUID.randomUUID();
    entity.setId(id);
    entity.setResource("res");
    entity.setAction("act");
    entity.setDescription("desc");

    Permission permission = mapper.toPermission(entity);

    assertNotNull(permission);
    assertEquals(id, permission.id());
    assertEquals("res", permission.resource());
    assertEquals("act", permission.action());
  }

  @Test
  void toUserRole() {
    UserRoleEntity entity = new UserRoleEntity();
    UUID id = UUID.randomUUID();
    entity.setId(id);
    entity.setUserId(UUID.randomUUID());
    entity.setAssignedAt(OffsetDateTime.now());

    UserRole userRole = mapper.toUserRole(entity);

    assertNotNull(userRole);
    assertEquals(id, userRole.id());
  }

  @Test
  void toRoleList() {
    RoleEntity entity = new RoleEntity();
    entity.setName("ADMIN");
    List<Role> roles = mapper.toRoleList(List.of(entity));

    assertEquals(1, roles.size());
    assertEquals("ADMIN", roles.get(0).name());
  }

  @Test
  void toPermissionList() {
    PermissionEntity entity = new PermissionEntity();
    entity.setResource("res");
    List<Permission> permissions = mapper.toPermissionList(List.of(entity));

    assertEquals(1, permissions.size());
    assertEquals("res", permissions.get(0).resource());
  }

  @Test
  void toPermissionEntity() {
    Permission permission =
        new Permission(UUID.randomUUID(), "res", "act", "desc", OffsetDateTime.now());
    PermissionEntity entity = mapper.toPermissionEntity(permission);
    assertNotNull(entity);
    assertEquals(permission.resource(), entity.getResource());
  }

  @Test
  void toUserRoleEntity() {
    UserRole userRole =
        new UserRole(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            "MEMBER",
            UUID.randomUUID(),
            OffsetDateTime.now());
    UserRoleEntity entity = mapper.toUserRoleEntity(userRole);
    assertNotNull(entity);
    assertEquals(userRole.userId(), entity.getUserId());
  }

  @Test
  void toUserRoleList() {
    UserRoleEntity entity = new UserRoleEntity();
    entity.setId(UUID.randomUUID());
    List<UserRole> list = mapper.toUserRoleList(List.of(entity));
    assertEquals(1, list.size());
  }

  @Test
  void toRoleEntity() {
    Role role =
        new Role(
            UUID.randomUUID(),
            "ADMIN",
            "Administrator",
            true,
            OffsetDateTime.now(),
            OffsetDateTime.now());

    RoleEntity entity = mapper.toRoleEntity(role);

    assertNotNull(entity);
    assertEquals(role.id(), entity.getId());
    assertEquals("ADMIN", entity.getName());
    assertTrue(entity.isSystem());
  }

  @Test
  void nullInputs() {
    assertNull(mapper.toRole((RoleEntity) null));
    assertNull(mapper.toRoleEntity(null));
    assertNull(mapper.toPermission((PermissionEntity) null));
    assertNull(mapper.toPermissionEntity(null));
    assertNull(mapper.toUserRole((UserRoleEntity) null));
    assertNull(mapper.toUserRoleEntity(null));
    assertNull(mapper.toRoleList(null));
    assertNull(mapper.toPermissionList(null));
    assertNull(mapper.toUserRoleList(null));
  }
}
