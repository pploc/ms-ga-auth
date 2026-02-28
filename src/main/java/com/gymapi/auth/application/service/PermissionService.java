package com.gymapi.auth.application.service;

import com.gymapi.auth.application.port.in.CreatePermissionCommand;
import com.gymapi.auth.application.port.in.PermissionUseCase;
import com.gymapi.auth.application.port.in.UpdatePermissionCommand;
import com.gymapi.auth.application.port.out.PermissionRepository;
import com.gymapi.auth.domain.exception.ConflictException;
import com.gymapi.auth.domain.exception.ResourceNotFoundException;
import com.gymapi.auth.domain.model.Permission;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PermissionService implements PermissionUseCase {

    private final PermissionRepository permissionRepository;

    @Override
    public Permission createPermission(CreatePermissionCommand command) {
        if (permissionRepository.existsByResourceAndAction(command.getResource(), command.getAction())) {
            throw new ConflictException("Permission Resource:Action already exists");
        }

        Permission permission = Permission.builder()
                .resource(command.getResource())
                .action(command.getAction())
                .description(command.getDescription())
                .build();

        return permissionRepository.save(permission);
    }

    @Override
    public List<Permission> getAllPermissions() {
        return permissionRepository.findAll();
    }

    @Override
    public Permission updatePermission(UUID id, UpdatePermissionCommand command) {
        Permission permission = permissionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Permission not found"));

        if (command.getDescription() != null) {
            permission.setDescription(command.getDescription());
        }

        return permissionRepository.save(permission);
    }

    @Override
    public void deletePermission(UUID id) {
        if (!permissionRepository.findById(id).isPresent()) {
            throw new ResourceNotFoundException("Permission not found");
        }
        permissionRepository.deleteById(id);
    }
}
