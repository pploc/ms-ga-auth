package com.gymapi.auth.adapter.in.web.controller;

import com.gymapi.auth.adapter.in.web.dto.request.AssignRoleRequest;
import com.gymapi.auth.adapter.in.web.dto.response.RolesWithPermissionsResponse;
import com.gymapi.auth.adapter.in.web.dto.response.UserRoleResponse;
import com.gymapi.auth.adapter.in.web.mapper.AuthWebMapper;
import com.gymapi.auth.application.port.in.UserRoleUseCase;
import com.gymapi.auth.domain.model.RolesWithPermissions;
import com.gymapi.auth.domain.model.UserRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/auth/users/{userId}/roles")
@RequiredArgsConstructor
@Tag(name = "User Role Management", description = "APIs for managing user roles")
public class UserRoleController {

    private final UserRoleUseCase userRoleUseCase;
    private final AuthWebMapper mapper;

    @PostMapping
    @Operation(summary = "Assign a role to a user")
    public ResponseEntity<Map<String, Object>> assignRole(
            @PathVariable UUID userId,
            @Valid @RequestBody AssignRoleRequest request) {
        UserRole userRole = userRoleUseCase.assignRole(
                userId,
                request.getRoleId(),
                request.getAssignedBy()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "message", "Role assigned.",
                "user_id", userId,
                "role_id", request.getRoleId()
        ));
    }

    @DeleteMapping("/{roleId}")
    @Operation(summary = "Revoke a role from a user")
    public ResponseEntity<Map<String, Object>> revokeRole(
            @PathVariable UUID userId,
            @PathVariable UUID roleId) {
        userRoleUseCase.revokeRole(userId, roleId);
        return ResponseEntity.ok(Map.of("message", "Role removed."));
    }

    @GetMapping
    @Operation(summary = "Get all roles for a user")
    public ResponseEntity<List<UserRoleResponse>> getUserRoles(@PathVariable UUID userId) {
        List<UserRole> userRoles = userRoleUseCase.getUserRoles(userId);
        return ResponseEntity.ok(mapper.toUserRoleResponseList(userRoles));
    }

    @GetMapping("/{roleId}/check")
    @Operation(summary = "Check if user has a specific role")
    public ResponseEntity<Boolean> hasRole(
            @PathVariable UUID userId,
            @PathVariable UUID roleId) {
        boolean hasRole = userRoleUseCase.hasRole(userId, roleId);
        return ResponseEntity.ok(hasRole);
    }

    @GetMapping("/with-permissions")
    @Operation(summary = "Get user's roles with permissions (internal use)")
    public ResponseEntity<RolesWithPermissionsResponse> getUserRolesWithPermissions(@PathVariable UUID userId) {
        RolesWithPermissions rolesWithPermissions = userRoleUseCase.getUserRolesWithPermissions(userId);
        RolesWithPermissionsResponse response = RolesWithPermissionsResponse.builder()
                .userId(rolesWithPermissions.userId())
                .roles(rolesWithPermissions.roles())
                .permissions(rolesWithPermissions.permissions())
                .build();
        return ResponseEntity.ok(response);
    }
}
