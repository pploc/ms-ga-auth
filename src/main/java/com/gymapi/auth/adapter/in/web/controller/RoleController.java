package com.gymapi.auth.adapter.in.web.controller;

import com.gymapi.auth.adapter.in.web.dto.generated.CreateRoleRequest;
import com.gymapi.auth.adapter.in.web.dto.generated.ErrorResponse;
import com.gymapi.auth.adapter.in.web.dto.generated.PermissionResponse;
import com.gymapi.auth.adapter.in.web.dto.generated.RoleResponse;
import com.gymapi.auth.adapter.in.web.dto.generated.SetPermissionsResponse;
import com.gymapi.auth.adapter.in.web.dto.generated.SetRolePermissionsRequest;
import com.gymapi.auth.adapter.in.web.dto.generated.UpdateRoleRequest;
import com.gymapi.auth.adapter.in.web.mapper.AuthWebMapper;
import com.gymapi.auth.application.port.in.RoleUseCase;
import com.gymapi.auth.domain.model.Permission;
import com.gymapi.auth.domain.model.Role;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.LinkedHashSet;
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
@RequestMapping("/auth/roles")
@RequiredArgsConstructor
@Tag(
    name = "Role Management",
    description =
        "Create, read, update and delete roles, and manage the permissions each role carries.")
public class RoleController {

  private final RoleUseCase roleUseCase;
  private final AuthWebMapper mapper;

  @PostMapping
  @Operation(
      summary = "Create a new role",
      description =
          """
          Creates a role. Role names are unique and case-sensitive.

          Setting `system` to `true` marks the role as platform-managed, which makes it \
          immutable through this API afterwards — subsequent updates, deletes and permission \
          changes are rejected with `SYSTEM_ROLE_IMMUTABLE`.""")
  @ApiResponse(responseCode = "201", description = "Role created.")
  @ApiResponse(
      responseCode = "409",
      description = "A role with that name already exists (`ROLE_ALREADY_EXISTS`).",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  public ResponseEntity<RoleResponse> createRole(@Valid @RequestBody CreateRoleRequest request) {
    Role role =
        roleUseCase.createRole(request.getName(), request.getDescription(), request.getSystem());
    return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toRoleResponse(role));
  }

  @PutMapping("/{id}")
  @Operation(
      summary = "Update a role",
      description =
          """
          Replaces the name and description of a non-system role. System roles are immutable \
          and are rejected with `SYSTEM_ROLE_IMMUTABLE`.""")
  @ApiResponse(responseCode = "200", description = "Role updated.")
  @ApiResponse(
      responseCode = "404",
      description = "No role exists with that id (`ROLE_NOT_FOUND`).",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(
      responseCode = "409",
      description = "Another role already uses the requested name (`ROLE_ALREADY_EXISTS`).",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  public ResponseEntity<RoleResponse> updateRole(
      @Parameter(description = "Role identifier (UUID).") @PathVariable UUID id,
      @Valid @RequestBody UpdateRoleRequest request) {
    Role role = roleUseCase.updateRole(id, request.getName(), request.getDescription());
    return ResponseEntity.ok(mapper.toRoleResponse(role));
  }

  @DeleteMapping("/{id}")
  @Operation(
      summary = "Delete a role",
      description =
          """
          Deletes a non-system role together with its permission mappings. Existing user \
          assignments for the role are removed by cascade.""")
  @ApiResponse(responseCode = "204", description = "Role deleted. No body.")
  @ApiResponse(
      responseCode = "404",
      description = "No role exists with that id (`ROLE_NOT_FOUND`).",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  public ResponseEntity<Void> deleteRole(
      @Parameter(description = "Role identifier (UUID).") @PathVariable UUID id) {
    roleUseCase.deleteRole(id);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get a role by ID")
  @ApiResponse(responseCode = "200", description = "The requested role.")
  @ApiResponse(
      responseCode = "404",
      description = "No role exists with that id (`ROLE_NOT_FOUND`).",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  public ResponseEntity<RoleResponse> getRoleById(
      @Parameter(description = "Role identifier (UUID).") @PathVariable UUID id) {
    Role role = roleUseCase.getRoleById(id);
    return ResponseEntity.ok(mapper.toRoleResponse(role));
  }

  @GetMapping("/name/{name}")
  @Operation(
      summary = "Get a role by name",
      description = "Case-sensitive lookup by the unique role name, e.g. `MEMBER`.")
  @ApiResponse(responseCode = "200", description = "The requested role.")
  @ApiResponse(
      responseCode = "404",
      description = "No role exists with that name (`ROLE_NOT_FOUND`).",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  public ResponseEntity<RoleResponse> getRoleByName(
      @Parameter(description = "Unique role name.", example = "MEMBER") @PathVariable String name) {
    Role role = roleUseCase.getRoleByName(name);
    return ResponseEntity.ok(mapper.toRoleResponse(role));
  }

  @GetMapping
  @Operation(
      summary = "List all roles",
      description = "Returns every role defined in the system, including seeded system roles.")
  @ApiResponse(responseCode = "200", description = "The full list of roles. Empty array when none.")
  public ResponseEntity<List<RoleResponse>> getAllRoles() {
    List<Role> roles = roleUseCase.getAllRoles();
    return ResponseEntity.ok(mapper.toRoleResponseList(roles));
  }

  @GetMapping("/{id}/permissions")
  @Operation(summary = "List the permissions granted to a role")
  @ApiResponse(
      responseCode = "200",
      description = "Permissions currently mapped to the role. Empty array when none.")
  @ApiResponse(
      responseCode = "404",
      description = "No role exists with that id (`ROLE_NOT_FOUND`).",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  public ResponseEntity<List<PermissionResponse>> getRolePermissions(
      @Parameter(description = "Role identifier (UUID).") @PathVariable UUID id) {
    List<Permission> permissions = roleUseCase.getRolePermissions(id);
    return ResponseEntity.ok(mapper.toPermissionResponseList(permissions));
  }

  @PutMapping("/{id}/permissions")
  @Operation(
      summary = "Replace the permissions granted to a role",
      description =
          """
          Full replacement, not a merge: the role ends up with exactly the permissions listed \
          in `permissionIds`. Send an empty array to revoke everything.

          Publishes an `auth.permission_changed` event on the `auth.events` Kafka topic. \
          Already-issued JWTs keep their embedded permissions until they expire.""")
  @ApiResponse(responseCode = "200", description = "Permissions replaced.")
  @ApiResponse(
      responseCode = "404",
      description =
          "The role does not exist (`ROLE_NOT_FOUND`), or one of the supplied permission ids"
              + " does not exist (`PERMISSION_NOT_FOUND`).",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  public ResponseEntity<SetPermissionsResponse> setRolePermissions(
      @Parameter(description = "Role identifier (UUID).") @PathVariable UUID id,
      @Valid @RequestBody SetRolePermissionsRequest request) {

    roleUseCase.setRolePermissions(id, new LinkedHashSet<>(request.getPermissionIds()));
    List<Permission> permissions = roleUseCase.getRolePermissions(id);

    return ResponseEntity.ok(
        new SetPermissionsResponse()
            .message("Permissions updated.")
            .permissionCount(permissions.size()));
  }
}
