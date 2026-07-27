package com.gymapi.auth.adapter.in.web.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gymapi.auth.adapter.in.web.advice.ErrorResponseFactory;
import com.gymapi.auth.adapter.in.web.dto.generated.AssignRoleRequest;
import com.gymapi.auth.adapter.in.web.mapper.AuthWebMapper;
import com.gymapi.auth.application.port.in.IdempotencyUseCase;
import com.gymapi.auth.application.port.in.UserRoleUseCase;
import com.gymapi.auth.domain.model.RolesWithPermissions;
import com.gymapi.auth.domain.model.UserRole;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserRoleController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(ErrorResponseFactory.class)
class UserRoleControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private UserRoleUseCase userRoleUseCase;

  @MockBean private AuthWebMapper mapper;

  // The idempotency filter is a @Component, so the slice instantiates it even though
  // addFilters = false keeps it out of the chain.
  @MockBean private IdempotencyUseCase idempotencyUseCase;

  @Autowired private ObjectMapper objectMapper;

  private UUID userId;
  private UUID roleId;
  private UserRole testUserRole;

  @BeforeEach
  void setUp() {
    userId = UUID.randomUUID();
    roleId = UUID.randomUUID();
    testUserRole =
        new UserRole(
            UUID.randomUUID(), userId, roleId, "MEMBER", UUID.randomUUID(), OffsetDateTime.now());
  }

  @Test
  void assignRole_Success() throws Exception {
    AssignRoleRequest request = new AssignRoleRequest();
    request.setRoleId(roleId);

    when(userRoleUseCase.assignRole(eq(userId), eq(roleId), any())).thenReturn(testUserRole);

    mockMvc
        .perform(
            post("/auth/users/{userId}/roles", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.message").value("Role assigned."))
        .andExpect(jsonPath("$.userId").value(userId.toString()))
        .andExpect(jsonPath("$.roleId").value(roleId.toString()));
  }

  @Test
  void revokeRole_Success() throws Exception {
    mockMvc
        .perform(delete("/auth/users/{userId}/roles/{roleId}", userId, roleId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("Role removed."));
  }

  @Test
  void getUserRolesWithPermissions_Success() throws Exception {
    RolesWithPermissions rolesWithPermissions =
        new RolesWithPermissions(userId, List.of("MEMBER"), List.of("booking:read"));
    com.gymapi.auth.adapter.in.web.dto.generated.RolesWithPermissionsResponse response =
        new com.gymapi.auth.adapter.in.web.dto.generated.RolesWithPermissionsResponse();
    response.setUserId(userId);
    response.setRoles(List.of("MEMBER"));
    response.setPermissions(List.of("booking:read"));

    when(userRoleUseCase.getUserRolesWithPermissions(userId)).thenReturn(rolesWithPermissions);
    when(mapper.toRolesWithPermissionsResponse(rolesWithPermissions)).thenReturn(response);

    mockMvc
        .perform(get("/auth/users/{userId}/roles/with-permissions", userId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.userId").value(userId.toString()))
        .andExpect(jsonPath("$.roles[0]").value("MEMBER"))
        .andExpect(jsonPath("$.permissions[0]").value("booking:read"));
  }

  @Test
  void getUserRoles_Success() throws Exception {
    when(userRoleUseCase.getUserRoles(userId)).thenReturn(List.of(testUserRole));
    when(mapper.toUserRoleResponseList(any()))
        .thenReturn(List.of(new com.gymapi.auth.adapter.in.web.dto.generated.UserRoleResponse()));

    mockMvc
        .perform(get("/auth/users/{userId}/roles", userId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray());
  }

  @Test
  void hasRole_Success() throws Exception {
    when(userRoleUseCase.hasRole(userId, roleId)).thenReturn(true);

    mockMvc
        .perform(get("/auth/users/{userId}/roles/{roleId}/check", userId, roleId))
        .andExpect(status().isOk())
        .andExpect(content().string("true"));
  }
}
