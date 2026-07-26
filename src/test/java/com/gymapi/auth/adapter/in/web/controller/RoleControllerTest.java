package com.gymapi.auth.adapter.in.web.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gymapi.auth.adapter.in.web.advice.ErrorResponseFactory;
import com.gymapi.auth.adapter.in.web.dto.generated.CreateRoleRequest;
import com.gymapi.auth.adapter.in.web.dto.generated.RoleResponse;
import com.gymapi.auth.adapter.in.web.dto.generated.UpdateRoleRequest;
import com.gymapi.auth.adapter.in.web.mapper.AuthWebMapper;
import com.gymapi.auth.application.port.in.IdempotencyUseCase;
import com.gymapi.auth.application.port.in.RoleUseCase;
import com.gymapi.auth.domain.model.Role;
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

@WebMvcTest(RoleController.class)
@AutoConfigureMockMvc(addFilters = false) // Disable security for controller tests
@Import(ErrorResponseFactory.class)
class RoleControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private RoleUseCase roleUseCase;

  @MockBean private AuthWebMapper mapper;

  // The idempotency filter is a @Component, so the slice instantiates it even though
  // addFilters = false keeps it out of the chain.
  @MockBean private IdempotencyUseCase idempotencyUseCase;

  @Autowired private ObjectMapper objectMapper;

  private Role testRole;
  private RoleResponse testRoleResponse;
  private UUID roleId;

  @BeforeEach
  void setUp() {
    roleId = UUID.randomUUID();
    testRole =
        new Role(roleId, "ADMIN", "Admin role", true, OffsetDateTime.now(), OffsetDateTime.now());
    testRoleResponse = new RoleResponse();
    testRoleResponse.setId(roleId);
    testRoleResponse.setName("ADMIN");
    testRoleResponse.setIsSystem(true);
  }

  @Test
  void createRole_Success() throws Exception {
    CreateRoleRequest request = new CreateRoleRequest();
    request.setName("USER");
    request.setSystem(false);

    when(roleUseCase.createRole(eq("USER"), anyString(), anyBoolean())).thenReturn(testRole);
    when(mapper.toRoleResponse(any())).thenReturn(testRoleResponse);

    mockMvc
        .perform(
            post("/auth/roles")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.name").value("ADMIN"));
  }

  @Test
  void getRoleById_Success() throws Exception {
    when(roleUseCase.getRoleById(roleId)).thenReturn(testRole);
    when(mapper.toRoleResponse(testRole)).thenReturn(testRoleResponse);

    mockMvc
        .perform(get("/auth/roles/{id}", roleId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(roleId.toString()));
  }

  @Test
  void updateRole_Success() throws Exception {
    UpdateRoleRequest request = new UpdateRoleRequest();
    request.setName("SUPER");

    when(roleUseCase.updateRole(eq(roleId), eq("SUPER"), any())).thenReturn(testRole);
    when(mapper.toRoleResponse(any())).thenReturn(testRoleResponse);

    mockMvc
        .perform(
            put("/auth/roles/{id}", roleId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk());
  }

  @Test
  void deleteRole_Success() throws Exception {
    mockMvc.perform(delete("/auth/roles/{id}", roleId)).andExpect(status().isNoContent());
  }

  @Test
  void getRoleByName_Success() throws Exception {
    when(roleUseCase.getRoleByName("ADMIN")).thenReturn(testRole);
    when(mapper.toRoleResponse(testRole)).thenReturn(testRoleResponse);

    mockMvc
        .perform(get("/auth/roles/name/{name}", "ADMIN"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("ADMIN"));
  }

  @Test
  void getRolePermissions_Success() throws Exception {
    when(roleUseCase.getRolePermissions(roleId)).thenReturn(List.of());

    mockMvc
        .perform(get("/auth/roles/{id}/permissions", roleId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray());
  }

  @Test
  void getAllRoles_Success() throws Exception {
    when(roleUseCase.getAllRoles()).thenReturn(List.of(testRole));
    when(mapper.toRoleResponseList(any())).thenReturn(List.of(testRoleResponse));

    mockMvc
        .perform(get("/auth/roles"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].name").value("ADMIN"));
  }

  @Test
  void setRolePermissions_Success() throws Exception {
    com.gymapi.auth.adapter.in.web.dto.generated.SetRolePermissionsRequest request =
        new com.gymapi.auth.adapter.in.web.dto.generated.SetRolePermissionsRequest();
    request.setPermissionIds(List.of(UUID.randomUUID()));

    when(roleUseCase.getRolePermissions(roleId)).thenReturn(List.of());

    mockMvc
        .perform(
            put("/auth/roles/{id}/permissions", roleId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("Permissions updated."))
        .andExpect(jsonPath("$.permissionCount").value(0));
  }
}
