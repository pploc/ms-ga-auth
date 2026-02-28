package com.gymapi.auth.application.service;

import com.gymapi.auth.application.port.in.PermissionUseCase;
import com.gymapi.auth.application.port.out.PermissionRepository;
import com.gymapi.auth.domain.exception.DuplicatePermissionException;
import com.gymapi.auth.domain.exception.PermissionNotFoundException;
import com.gymapi.auth.domain.model.Permission;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PermissionService implements PermissionUseCase {

    private static final String PERMISSION_ALREADY_EXISTS = "Permission with resource '%s' and action '%s' already exists";
    private static final String PERMISSION_NOT_FOUND = "Permission not found with id: %s";

    private final PermissionRepository permissionRepository;

    @Override
    @Transactional
    public Permission createPermission(String resource, String action, String description) {
        if (permissionRepository.existsByResourceAndAction(resource, action)) {
            throw new DuplicatePermissionException(String.format(PERMISSION_ALREADY_EXISTS, resource, action));
        }

        Permission permission = new Permission(
                null,
                resource,
                action,
                description,
                OffsetDateTime.now()
        );

        return permissionRepository.save(permission);
    }

    @Override
    @Transactional
    public Permission updatePermission(UUID id, String resource, String action, String description) {
        Permission existingPermission = permissionRepository.findById(id)
                .orElseThrow(() -> new PermissionNotFoundException(String.format(PERMISSION_NOT_FOUND, id)));

        if (!existingPermission.resource().equals(resource) || !existingPermission.action().equals(action)) {
            if (permissionRepository.existsByResourceAndAction(resource, action)) {
                throw new DuplicatePermissionException(String.format(PERMISSION_ALREADY_EXISTS, resource, action));
            }
        }

        Permission updatedPermission = new Permission(
                existingPermission.id(),
                resource,
                action,
                description,
                existingPermission.createdAt()
        );

        return permissionRepository.save(updatedPermission);
    }

    @Override
    @Transactional
    public void deletePermission(UUID id) {
        if (!permissionRepository.existsByResourceAndAction("", "")) {
            permissionRepository.findById(id)
                    .orElseThrow(() -> new PermissionNotFoundException(String.format(PERMISSION_NOT_FOUND, id)));
        }
        permissionRepository.deleteById(id);
    }

    @Override
    public Permission getPermissionById(UUID id) {
        return permissionRepository.findById(id)
                .orElseThrow(() -> new PermissionNotFoundException(String.format(PERMISSION_NOT_FOUND, id)));
    }

    @Override
    public Permission getPermissionByResourceAndAction(String resource, String action) {
        return permissionRepository.findByResourceAndAction(resource, action)
                .orElseThrow(() -> new PermissionNotFoundException(
                        "Permission not found with resource: " + resource + " and action: " + action));
    }

    @Override
    public List<Permission> getAllPermissions() {
        return permissionRepository.findAll();
    }
}
