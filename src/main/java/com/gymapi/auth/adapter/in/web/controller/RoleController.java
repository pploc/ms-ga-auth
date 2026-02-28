package com.gymapi.auth.adapter.in.web.controller;

import com.gymapi.auth.adapter.in.web.dto.request.CreateRoleRequest;
import com.gymapi.auth.adapter.in.web.dto.request.UpdateRoleRequest;
import com.gymapi.auth.adapter.in.web.dto.response.RoleResponse;
import com.gymapi.auth.adapter.in.web.mapper.AuthWebMapper;
import com.gymapi.auth.application.port.in.RoleUseCase;
import com.gymapi.auth.domain.model.Role;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/auth/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleUseCase roleUseCase;
    private final AuthWebMapper mapper;

    @PostMapping
    public ResponseEntity<RoleResponse> createRole(@Valid @RequestBody CreateRoleRequest request) {
        Role role = roleUseCase.createRole(
                request.getName(),
                request.getDescription(),
                request.isSystem()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toRoleResponse(role));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RoleResponse> updateRole(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateRoleRequest request) {
        Role role = roleUseCase.updateRole(id, request.getName(), request.getDescription());
        return ResponseEntity.ok(mapper.toRoleResponse(role));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRole(@PathVariable UUID id) {
        roleUseCase.deleteRole(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<RoleResponse> getRoleById(@PathVariable UUID id) {
        Role role = roleUseCase.getRoleById(id);
        return ResponseEntity.ok(mapper.toRoleResponse(role));
    }

    @GetMapping("/name/{name}")
    public ResponseEntity<RoleResponse> getRoleByName(@PathVariable String name) {
        Role role = roleUseCase.getRoleByName(name);
        return ResponseEntity.ok(mapper.toRoleResponse(role));
    }

    @GetMapping
    public ResponseEntity<List<RoleResponse>> getAllRoles() {
        List<Role> roles = roleUseCase.getAllRoles();
        return ResponseEntity.ok(mapper.toRoleResponseList(roles));
    }
}
