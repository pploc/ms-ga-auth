package com.gymapi.auth.adapter.in.web.controller;

import com.gymapi.auth.adapter.in.web.dto.generated.CreateRoleRequest;
import com.gymapi.auth.adapter.in.web.dto.generated.SetRolePermissionsRequest;
import com.gymapi.auth.adapter.in.web.dto.generated.UpdateRoleRequest;
import com.gymapi.auth.adapter.in.web.dto.generated.PermissionResponse;
import com.gymapi.auth.adapter.in.web.dto.generated.RoleResponse;
import com.gymapi.auth.adapter.in.web.mapper.AuthWebMapper;
import com.gymapi.auth.application.port.in.RoleUseCase;
import com.gymapi.auth.domain.model.Permission;
import com.gymapi.auth.domain.model.Role;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/auth/roles")
@RequiredArgsConstructor
@Tag(name = "Role Management", description = "APIs for managing roles")
public class RoleController {

    private final RoleUseCase roleUseCase;
    private final AuthWebMapper mapper;

    @PostMapping
    @Operation(summary = "Create a new role")
    public ResponseEntity<RoleResponse> createRole(@Valid @RequestBody CreateRoleRequest request) {
        Role role = roleUseCase.createRole(
                request.getName(),
                request.getDescription(),
                request.getSystem()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toRoleResponse(role));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a role")
    public ResponseEntity<RoleResponse> updateRole(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateRoleRequest request) {
        Role role = roleUseCase.updateRole(id, request.getName(), request.getDescription());
        return ResponseEntity.ok(mapper.toRoleResponse(role));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a role")
    public ResponseEntity<Void> deleteRole(@PathVariable UUID id) {
        roleUseCase.deleteRole(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get role by ID")
    public ResponseEntity<RoleResponse> getRoleById(@PathVariable UUID id) {
        Role role = roleUseCase.getRoleById(id);
        return ResponseEntity.ok(mapper.toRoleResponse(role));
    }

    @GetMapping("/name/{name}")
    @Operation(summary = "Get role by name")
    public ResponseEntity<RoleResponse> getRoleByName(@PathVariable String name) {
        Role role = roleUseCase.getRoleByName(name);
        return ResponseEntity.ok(mapper.toRoleResponse(role));
    }

    @GetMapping
    @Operation(summary = "Get all roles")
    public ResponseEntity<List<RoleResponse>> getAllRoles() {
        List<Role> roles = roleUseCase.getAllRoles();
        return ResponseEntity.ok(mapper.toRoleResponseList(roles));
    }

    @GetMapping("/{id}/permissions")
    @Operation(summary = "Get permissions for a role")
    public ResponseEntity<List<PermissionResponse>> getRolePermissions(@PathVariable UUID id) {
        List<Permission> permissions = roleUseCase.getRolePermissions(id);
        return ResponseEntity.ok(mapper.toPermissionResponseList(permissions));
    }

    @PutMapping("/{id}/permissions")
    @Operation(summary = "Set permissions for a role")
    public ResponseEntity<Map<String, Object>> setRolePermissions(
            @PathVariable UUID id,
            @Valid @RequestBody SetRolePermissionsRequest request) {
        roleUseCase.setRolePermissions(id, new HashSet<>(request.getPermissionIds()));
        List<Permission> permissions = roleUseCase.getRolePermissions(id);
        return ResponseEntity.ok(Map.of(
                "message", "Permissions updated.",
                "permission_count", permissions.size()
        ));
    }
}
