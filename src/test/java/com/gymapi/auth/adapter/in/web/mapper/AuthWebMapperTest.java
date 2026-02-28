package com.gymapi.auth.adapter.in.web.mapper;

import com.gymapi.auth.adapter.in.web.dto.generated.*;
import com.gymapi.auth.domain.model.Permission;
import com.gymapi.auth.domain.model.Role;
import com.gymapi.auth.domain.model.RolesWithPermissions;
import com.gymapi.auth.domain.model.UserRole;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AuthWebMapperTest {

    private final AuthWebMapper mapper = Mappers.getMapper(AuthWebMapper.class);

    @Test
    void toRole_FromCreateRoleRequest() {
        CreateRoleRequest request = new CreateRoleRequest();
        request.setName("TEST_ROLE");
        request.setDescription("Test description");
        request.setSystem(false);

        Role role = mapper.toRole(request);

        assertNotNull(role);
        assertEquals("TEST_ROLE", role.name());
        assertEquals("Test description", role.description());
        assertFalse(role.isSystem());
    }

    @Test
    void toRole_FromUpdateRoleRequest() {
        UpdateRoleRequest request = new UpdateRoleRequest();
        request.setName("UPDATED");
        request.setDescription("Updated description");

        Role role = mapper.toRole(request);

        assertNotNull(role);
        assertEquals("UPDATED", role.name());
        assertEquals("Updated description", role.description());
    }

    @Test
    void toRoleResponse() {
        Role role = new Role(
                UUID.randomUUID(),
                "ROLE_ADMIN",
                "Admin role",
                true,
                OffsetDateTime.now(),
                OffsetDateTime.now());

        RoleResponse response = mapper.toRoleResponse(role);

        assertNotNull(response);
        assertEquals(role.id(), response.getId());
        assertEquals("ROLE_ADMIN", response.getName());
        assertEquals("Admin role", response.getDescription());
        assertTrue(response.getIsSystem());
    }

    @Test
    void toPermission() {
        CreatePermissionRequest request = new CreatePermissionRequest();
        request.setResource("booking");
        request.setAction("read");
        request.setDescription("Read bookings");

        Permission permission = mapper.toPermission(request);

        assertNotNull(permission);
        assertEquals("booking", permission.resource());
        assertEquals("read", permission.action());
        assertEquals("Read bookings", permission.description());
    }

    @Test
    void toUserRoleResponse() {
        UUID userId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        UserRole userRole = new UserRole(UUID.randomUUID(), userId, roleId, UUID.randomUUID(), OffsetDateTime.now());

        UserRoleResponse response = mapper.toUserRoleResponse(userRole);

        assertNotNull(response);
        assertEquals(userId, response.getUserId());
        assertEquals(roleId, response.getRoleId());
    }

    @Test
    void toRoleResponseList() {
        Role role = new Role(UUID.randomUUID(), "ROLE", "Desc", false, OffsetDateTime.now(), OffsetDateTime.now());
        List<RoleResponse> result = mapper.toRoleResponseList(List.of(role));

        assertEquals(1, result.size());
        assertEquals(role.id(), result.get(0).getId());
    }

    @Test
    void toPermission_FromUpdatePermissionRequest() {
        UpdatePermissionRequest request = new UpdatePermissionRequest();
        request.setDescription("Updated");

        Permission permission = mapper.toPermission(request);
        assertNotNull(permission);
        assertEquals("Updated", permission.description());
    }

    @Test
    void toPermissionResponse() {
        Permission permission = new Permission(UUID.randomUUID(), "res", "act", "desc", OffsetDateTime.now());
        PermissionResponse response = mapper.toPermissionResponse(permission);
        assertNotNull(response);
        assertEquals(permission.id(), response.getId());
    }

    @Test
    void toPermissionResponseList() {
        Permission permission = new Permission(UUID.randomUUID(), "res", "act", "desc", OffsetDateTime.now());
        List<PermissionResponse> list = mapper.toPermissionResponseList(List.of(permission));
        assertEquals(1, list.size());
    }

    @Test
    void toUserRole_FromAssignRoleRequest() {
        AssignRoleRequest request = new AssignRoleRequest();
        request.setRoleId(UUID.randomUUID());
        request.setAssignedBy(UUID.randomUUID());

        UserRole userRole = mapper.toUserRole(request);
        assertNotNull(userRole);
        assertEquals(request.getRoleId(), userRole.roleId());
    }

    @Test
    void toUserRoleResponseList() {
        UserRole userRole = new UserRole(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                OffsetDateTime.now());
        List<UserRoleResponse> list = mapper.toUserRoleResponseList(List.of(userRole));
        assertEquals(1, list.size());
    }

    @Test
    void toRolesWithPermissionsResponse() {
        UUID userId = UUID.randomUUID();
        RolesWithPermissions domain = new RolesWithPermissions(userId, List.of("MEMBER"), List.of("read"));
        com.gymapi.auth.adapter.in.web.dto.generated.RolesWithPermissionsResponse response = mapper
                .toRolesWithPermissionsResponse(domain);

        assertNotNull(response);
        assertEquals(userId, response.getUserId());
        assertEquals("MEMBER", response.getRoles().get(0));
    }

    @Test
    void nullInputs() {
        assertNull(mapper.toRole((CreateRoleRequest) null));
        assertNull(mapper.toRole((UpdateRoleRequest) null));
        assertNull(mapper.toRoleResponse(null));
        assertNull(mapper.toRoleResponseList(null));
        assertNull(mapper.toPermission((CreatePermissionRequest) null));
        assertNull(mapper.toPermission((UpdatePermissionRequest) null));
        assertNull(mapper.toPermissionResponse(null));
        assertNull(mapper.toPermissionResponseList(null));
        assertNull(mapper.toUserRole(null));
        assertNull(mapper.toUserRoleResponse(null));
        assertNull(mapper.toUserRoleResponseList(null));
        assertNull(mapper.toRolesWithPermissionsResponse(null));
    }
}
