package com.gymapi.auth.adapter.in.web.controller;

import com.gymapi.auth.adapter.in.web.dto.generated.AssignRoleRequest;
import com.gymapi.auth.adapter.in.web.dto.generated.AssignRoleResponse;
import com.gymapi.auth.adapter.in.web.dto.generated.ErrorResponse;
import com.gymapi.auth.adapter.in.web.dto.generated.MessageResponse;
import com.gymapi.auth.adapter.in.web.dto.generated.RolesWithPermissionsResponse;
import com.gymapi.auth.adapter.in.web.dto.generated.UserRoleResponse;
import com.gymapi.auth.adapter.in.web.mapper.AuthWebMapper;
import com.gymapi.auth.application.port.in.UserRoleUseCase;
import com.gymapi.auth.domain.model.RolesWithPermissions;
import com.gymapi.auth.domain.model.UserRole;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth/users/{userId}/roles")
@RequiredArgsConstructor
@Tag(
    name = "User Role Management",
    description =
        "Assign and revoke roles for a user, and resolve the effective permission set used at"
            + " login.")
public class UserRoleController {

  private final UserRoleUseCase userRoleUseCase;
  private final AuthWebMapper mapper;

  @PostMapping
  @Operation(
      summary = "Assign a role to a user",
      description =
          """
          Idempotency is not implied: re-assigning a role the user already holds is rejected \
          with `USER_ROLE_ALREADY_ASSIGNED`.

          Publishes an `auth.role_assigned` event on the `auth.events` Kafka topic. The user's \
          next login picks up the new permissions; JWTs already issued do not.""")
  @ApiResponse(responseCode = "201", description = "Role assigned.")
  @ApiResponse(
      responseCode = "404",
      description = "No role exists with that id (`ROLE_NOT_FOUND`).",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(
      responseCode = "409",
      description = "The user already holds that role (`USER_ROLE_ALREADY_ASSIGNED`).",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  public ResponseEntity<AssignRoleResponse> assignRole(
      @Parameter(description = "User identifier (UUID).") @PathVariable UUID userId,
      @Valid @RequestBody AssignRoleRequest request) {

    userRoleUseCase.assignRole(userId, request.getRoleId(), request.getAssignedBy());

    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            new AssignRoleResponse()
                .message("Role assigned.")
                .userId(userId)
                .roleId(request.getRoleId()));
  }

  @DeleteMapping("/{roleId}")
  @Operation(
      summary = "Revoke a role from a user",
      description =
          """
          Removes the assignment and publishes an `auth.role_revoked` event on the \
          `auth.events` Kafka topic. Revocation does **not** invalidate JWTs that already embed \
          the permission — those expire on their own or must be blacklisted through \
          ms-ga-identifier.""")
  @ApiResponse(responseCode = "200", description = "Role revoked.")
  @ApiResponse(
      responseCode = "404",
      description = "The user does not hold that role (`USER_ROLE_NOT_FOUND`).",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  public ResponseEntity<MessageResponse> revokeRole(
      @Parameter(description = "User identifier (UUID).") @PathVariable UUID userId,
      @Parameter(description = "Role identifier (UUID).") @PathVariable UUID roleId) {
    userRoleUseCase.revokeRole(userId, roleId);
    return ResponseEntity.ok(new MessageResponse().message("Role removed."));
  }

  @GetMapping
  @Operation(
      summary = "List the roles assigned to a user",
      description =
          """
          Returns the raw assignments. An unknown user is not an error — it yields an empty \
          array, since this service does not own the user directory.""")
  @ApiResponse(
      responseCode = "200",
      description = "Assignments held by the user. Empty array when none.")
  public ResponseEntity<List<UserRoleResponse>> getUserRoles(
      @Parameter(description = "User identifier (UUID).") @PathVariable UUID userId) {
    List<UserRole> userRoles = userRoleUseCase.getUserRoles(userId);
    return ResponseEntity.ok(mapper.toUserRoleResponseList(userRoles));
  }

  @GetMapping("/{roleId}/check")
  @Operation(
      summary = "Check whether a user holds a role",
      description = "Returns a bare boolean. A missing user or role yields `false`, not a 404.")
  @ApiResponse(
      responseCode = "200",
      description = "`true` when the assignment exists, otherwise `false`.")
  public ResponseEntity<Boolean> hasRole(
      @Parameter(description = "User identifier (UUID).") @PathVariable UUID userId,
      @Parameter(description = "Role identifier (UUID).") @PathVariable UUID roleId) {
    return ResponseEntity.ok(userRoleUseCase.hasRole(userId, roleId));
  }

  @GetMapping("/with-permissions")
  @Operation(
      summary = "Resolve a user's roles and effective permissions",
      description =
          """
          **Internal endpoint.** Called by ms-ga-identifier during login to build the fat JWT. \
          Returns role names plus the de-duplicated union of the `resource:action` strings \
          those roles grant.

          An unknown user yields empty arrays rather than a 404 — this service does not own the \
          user directory, and a user with no assignments is a valid state.""")
  @ApiResponse(
      responseCode = "200",
      description = "The user's roles and their flattened permission set.")
  public ResponseEntity<RolesWithPermissionsResponse> getUserRolesWithPermissions(
      @Parameter(description = "User identifier (UUID).") @PathVariable UUID userId) {
    RolesWithPermissions rolesWithPermissions = userRoleUseCase.getUserRolesWithPermissions(userId);
    return ResponseEntity.ok(mapper.toRolesWithPermissionsResponse(rolesWithPermissions));
  }
}
