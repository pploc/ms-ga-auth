package com.gymapi.auth.adapter.in.web.controller;

import com.gymapi.auth.adapter.in.web.dto.generated.CreatePermissionRequest;
import com.gymapi.auth.adapter.in.web.dto.generated.UpdatePermissionRequest;
import com.gymapi.auth.adapter.in.web.dto.generated.PermissionResponse;
import com.gymapi.auth.adapter.in.web.mapper.AuthWebMapper;
import com.gymapi.auth.application.port.in.PermissionUseCase;
import com.gymapi.auth.domain.model.Permission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/auth/permissions")
@RequiredArgsConstructor
@Tag(name = "Permission Management", description = "APIs for managing permissions")
public class PermissionController {

    private final PermissionUseCase permissionUseCase;
    private final AuthWebMapper mapper;

    @PostMapping
    @Operation(summary = "Create a new permission")
    public ResponseEntity<PermissionResponse> createPermission(@Valid @RequestBody CreatePermissionRequest request) {
        Permission permission = permissionUseCase.createPermission(
                request.getResource(),
                request.getAction(),
                request.getDescription()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toPermissionResponse(permission));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a permission")
    public ResponseEntity<PermissionResponse> updatePermission(
            @PathVariable UUID id,
            @Valid @RequestBody UpdatePermissionRequest request) {
        Permission permission = permissionUseCase.updatePermission(
                id,
                request.getResource(),
                request.getAction(),
                request.getDescription()
        );
        return ResponseEntity.ok(mapper.toPermissionResponse(permission));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a permission")
    public ResponseEntity<Void> deletePermission(@PathVariable UUID id) {
        permissionUseCase.deletePermission(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get permission by ID")
    public ResponseEntity<PermissionResponse> getPermissionById(@PathVariable UUID id) {
        Permission permission = permissionUseCase.getPermissionById(id);
        return ResponseEntity.ok(mapper.toPermissionResponse(permission));
    }

    @GetMapping("/resource/{resource}/action/{action}")
    @Operation(summary = "Get permission by resource and action")
    public ResponseEntity<PermissionResponse> getPermissionByResourceAndAction(
            @PathVariable String resource,
            @PathVariable String action) {
        Permission permission = permissionUseCase.getPermissionByResourceAndAction(resource, action);
        return ResponseEntity.ok(mapper.toPermissionResponse(permission));
    }

    @GetMapping
    @Operation(summary = "Get all permissions")
    public ResponseEntity<List<PermissionResponse>> getAllPermissions() {
        List<Permission> permissions = permissionUseCase.getAllPermissions();
        return ResponseEntity.ok(mapper.toPermissionResponseList(permissions));
    }
}
