package com.gymapi.auth.adapter.in.web.controller;

import com.gymapi.auth.adapter.in.web.dto.request.AssignRoleRequest;
import com.gymapi.auth.adapter.in.web.dto.response.UserRoleResponse;
import com.gymapi.auth.adapter.in.web.mapper.AuthWebMapper;
import com.gymapi.auth.application.port.in.UserRoleUseCase;
import com.gymapi.auth.domain.model.UserRole;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/auth/users/{userId}/roles")
@RequiredArgsConstructor
public class UserRoleController {

    private final UserRoleUseCase userRoleUseCase;
    private final AuthWebMapper mapper;

    @PostMapping
    public ResponseEntity<UserRoleResponse> assignRole(
            @PathVariable UUID userId,
            @Valid @RequestBody AssignRoleRequest request) {
        UserRole userRole = userRoleUseCase.assignRole(
                userId,
                request.getRoleId(),
                request.getAssignedBy()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toUserRoleResponse(userRole));
    }

    @DeleteMapping("/{roleId}")
    public ResponseEntity<Void> revokeRole(
            @PathVariable UUID userId,
            @PathVariable UUID roleId) {
        userRoleUseCase.revokeRole(userId, roleId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<UserRoleResponse>> getUserRoles(@PathVariable UUID userId) {
        List<UserRole> userRoles = userRoleUseCase.getUserRoles(userId);
        return ResponseEntity.ok(mapper.toUserRoleResponseList(userRoles));
    }

    @GetMapping("/{roleId}/check")
    public ResponseEntity<Boolean> hasRole(
            @PathVariable UUID userId,
            @PathVariable UUID roleId) {
        boolean hasRole = userRoleUseCase.hasRole(userId, roleId);
        return ResponseEntity.ok(hasRole);
    }
}
