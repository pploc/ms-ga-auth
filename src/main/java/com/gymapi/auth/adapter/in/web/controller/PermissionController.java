package com.gymapi.auth.adapter.in.web.controller;

import com.gymapi.auth.adapter.in.web.dto.generated.CreatePermissionRequest;
import com.gymapi.auth.adapter.in.web.dto.generated.ErrorResponse;
import com.gymapi.auth.adapter.in.web.dto.generated.PermissionResponse;
import com.gymapi.auth.adapter.in.web.dto.generated.UpdatePermissionRequest;
import com.gymapi.auth.adapter.in.web.mapper.AuthWebMapper;
import com.gymapi.auth.application.port.in.PermissionUseCase;
import com.gymapi.auth.domain.model.Permission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth/permissions")
@RequiredArgsConstructor
@Tag(
    name = "Permission Management",
    description =
        "Create, read, update and delete the `resource:action` permissions roles can be granted.")
public class PermissionController {

  private final PermissionUseCase permissionUseCase;
  private final AuthWebMapper mapper;

  @PostMapping
  @Operation(
      summary = "Create a new permission",
      description =
          """
          Creates a `resource:action` grant. The pair is unique — creating a permission that \
          already exists is rejected with `PERMISSION_ALREADY_EXISTS`.""")
  @ApiResponse(responseCode = "201", description = "Permission created.")
  @ApiResponse(
      responseCode = "409",
      description = "That `resource:action` pair already exists (`PERMISSION_ALREADY_EXISTS`).",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  public ResponseEntity<PermissionResponse> createPermission(
      @Valid @RequestBody CreatePermissionRequest request) {
    Permission permission =
        permissionUseCase.createPermission(
            request.getResource(), request.getAction(), request.getDescription());
    return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toPermissionResponse(permission));
  }

  @PutMapping("/{id}")
  @Operation(
      summary = "Update a permission",
      description =
          """
          Replaces the resource, action and description. Renaming onto an existing \
          `resource:action` pair is rejected with `PERMISSION_ALREADY_EXISTS`.""")
  @ApiResponse(responseCode = "200", description = "Permission updated.")
  @ApiResponse(
      responseCode = "404",
      description = "No permission exists with that id (`PERMISSION_NOT_FOUND`).",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(
      responseCode = "409",
      description = "Another permission already uses that `resource:action` pair.",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  public ResponseEntity<PermissionResponse> updatePermission(
      @Parameter(description = "Permission identifier (UUID).") @PathVariable UUID id,
      @Valid @RequestBody UpdatePermissionRequest request) {
    Permission permission =
        permissionUseCase.updatePermission(
            id, request.getResource(), request.getAction(), request.getDescription());
    return ResponseEntity.ok(mapper.toPermissionResponse(permission));
  }

  @DeleteMapping("/{id}")
  @Operation(
      summary = "Delete a permission",
      description = "Deletes the permission and removes it from every role that carries it.")
  @ApiResponse(responseCode = "204", description = "Permission deleted. No body.")
  @ApiResponse(
      responseCode = "404",
      description = "No permission exists with that id (`PERMISSION_NOT_FOUND`).",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  public ResponseEntity<Void> deletePermission(
      @Parameter(description = "Permission identifier (UUID).") @PathVariable UUID id) {
    permissionUseCase.deletePermission(id);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get a permission by ID")
  @ApiResponse(responseCode = "200", description = "The requested permission.")
  @ApiResponse(
      responseCode = "404",
      description = "No permission exists with that id (`PERMISSION_NOT_FOUND`).",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  public ResponseEntity<PermissionResponse> getPermissionById(
      @Parameter(description = "Permission identifier (UUID).") @PathVariable UUID id) {
    Permission permission = permissionUseCase.getPermissionById(id);
    return ResponseEntity.ok(mapper.toPermissionResponse(permission));
  }

  @GetMapping("/resource/{resource}/action/{action}")
  @Operation(
      summary = "Get a permission by resource and action",
      description = "Lookup by the natural key, e.g. `booking` + `create` for `booking:create`.")
  @ApiResponse(responseCode = "200", description = "The requested permission.")
  @ApiResponse(
      responseCode = "404",
      description = "No permission exists for that pair (`PERMISSION_NOT_FOUND`).",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  public ResponseEntity<PermissionResponse> getPermissionByResourceAndAction(
      @Parameter(description = "Resource segment of the permission.", example = "booking")
          @PathVariable
          String resource,
      @Parameter(description = "Action segment of the permission.", example = "create")
          @PathVariable
          String action) {
    Permission permission = permissionUseCase.getPermissionByResourceAndAction(resource, action);
    return ResponseEntity.ok(mapper.toPermissionResponse(permission));
  }

  @GetMapping
  @Operation(summary = "List all permissions")
  @ApiResponse(
      responseCode = "200",
      description = "The full permission catalogue. Empty array when none exist.")
  public ResponseEntity<List<PermissionResponse>> getAllPermissions() {
    List<Permission> permissions = permissionUseCase.getAllPermissions();
    return ResponseEntity.ok(mapper.toPermissionResponseList(permissions));
  }
}
