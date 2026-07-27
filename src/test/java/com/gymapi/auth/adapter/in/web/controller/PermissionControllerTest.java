package com.gymapi.auth.adapter.in.web.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gymapi.auth.adapter.in.web.advice.ErrorResponseFactory;
import com.gymapi.auth.adapter.in.web.dto.generated.CreatePermissionRequest;
import com.gymapi.auth.adapter.in.web.dto.generated.PermissionResponse;
import com.gymapi.auth.adapter.in.web.mapper.AuthWebMapper;
import com.gymapi.auth.application.port.in.IdempotencyUseCase;
import com.gymapi.auth.application.port.in.PermissionUseCase;
import com.gymapi.auth.domain.exception.PermissionNotFoundException;
import com.gymapi.auth.domain.model.Permission;
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

@WebMvcTest(PermissionController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(ErrorResponseFactory.class)
class PermissionControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private PermissionUseCase permissionUseCase;

  @MockBean private AuthWebMapper mapper;

  // The idempotency filter is a @Component, so the slice instantiates it even though
  // addFilters = false keeps it out of the chain.
  @MockBean private IdempotencyUseCase idempotencyUseCase;

  @Autowired private ObjectMapper objectMapper;

  private Permission testPermission;
  private PermissionResponse testPermissionResponse;
  private UUID permissionId;

  @BeforeEach
  void setUp() {
    permissionId = UUID.randomUUID();
    testPermission =
        new Permission(permissionId, "booking", "read", "Read bookings", OffsetDateTime.now());
    testPermissionResponse = new PermissionResponse();
    testPermissionResponse.setId(permissionId);
    testPermissionResponse.setResource("booking");
    testPermissionResponse.setAction("read");
  }

  @Test
  void createPermission_Success() throws Exception {
    CreatePermissionRequest request = new CreatePermissionRequest();
    request.setResource("exercise");
    request.setAction("create");

    when(permissionUseCase.createPermission(eq("exercise"), eq("create"), any()))
        .thenReturn(testPermission);
    when(mapper.toPermissionResponse(any())).thenReturn(testPermissionResponse);

    mockMvc
        .perform(
            post("/auth/permissions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.resource").value("booking")); // Return testPermission mapping
  }

  @Test
  void getPermissionById_Success() throws Exception {
    when(permissionUseCase.getPermissionById(permissionId)).thenReturn(testPermission);
    when(mapper.toPermissionResponse(testPermission)).thenReturn(testPermissionResponse);

    mockMvc
        .perform(get("/auth/permissions/{id}", permissionId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(permissionId.toString()));
  }

  @Test
  void getPermissionById_NotFound() throws Exception {
    when(permissionUseCase.getPermissionById(permissionId))
        .thenThrow(new PermissionNotFoundException("not found"));

    mockMvc
        .perform(get("/auth/permissions/{id}", permissionId))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error").value("Not Found"));
  }

  @Test
  void updatePermission_Success() throws Exception {
    com.gymapi.auth.adapter.in.web.dto.generated.UpdatePermissionRequest request =
        new com.gymapi.auth.adapter.in.web.dto.generated.UpdatePermissionRequest();
    request.setResource("booking");
    request.setAction("update");
    request.setDescription("New desc");

    when(permissionUseCase.updatePermission(
            eq(permissionId), anyString(), anyString(), anyString()))
        .thenReturn(testPermission);
    when(mapper.toPermissionResponse(any())).thenReturn(testPermissionResponse);

    mockMvc
        .perform(
            put("/auth/permissions/{id}", permissionId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk());
  }

  @Test
  void deletePermission_Success() throws Exception {
    mockMvc
        .perform(delete("/auth/permissions/{id}", permissionId))
        .andExpect(status().isNoContent());
  }

  @Test
  void getPermissionByResourceAndAction_Success() throws Exception {
    when(permissionUseCase.getPermissionByResourceAndAction("booking", "read"))
        .thenReturn(testPermission);
    when(mapper.toPermissionResponse(testPermission)).thenReturn(testPermissionResponse);

    mockMvc
        .perform(get("/auth/permissions/resource/{resource}/action/{action}", "booking", "read"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.resource").value("booking"));
  }

  @Test
  void getAllPermissions_Success() throws Exception {
    when(permissionUseCase.getAllPermissions()).thenReturn(List.of(testPermission));
    when(mapper.toPermissionResponseList(any())).thenReturn(List.of(testPermissionResponse));

    mockMvc
        .perform(get("/auth/permissions"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].resource").value("booking"));
  }
}
