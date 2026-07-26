package com.gymapi.auth.application.service;

import com.gymapi.auth.application.port.in.PermissionUseCase;
import com.gymapi.auth.application.port.out.PermissionRepository;
import com.gymapi.auth.domain.exception.DuplicatePermissionException;
import com.gymapi.auth.domain.exception.PermissionNotFoundException;
import com.gymapi.auth.domain.model.Permission;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PermissionService implements PermissionUseCase {

  private final PermissionRepository permissionRepository;

  @Override
  @Transactional
  public Permission createPermission(String resource, String action, String description) {
    requireUniquePair(resource, action);

    Permission permission =
        Permission.builder()
            .resource(resource)
            .action(action)
            .description(description)
            .createdAt(OffsetDateTime.now())
            .build();

    return permissionRepository.save(permission);
  }

  @Override
  @Transactional
  public Permission updatePermission(UUID id, String resource, String action, String description) {
    Permission existing = requirePermission(id);

    boolean pairChanged =
        !existing.resource().equals(resource) || !existing.action().equals(action);
    if (pairChanged) {
      requireUniquePair(resource, action);
    }

    return permissionRepository.save(
        existing.toBuilder().resource(resource).action(action).description(description).build());
  }

  @Override
  @Transactional
  public void deletePermission(UUID id) {
    requirePermission(id);
    permissionRepository.deleteById(id);
  }

  @Override
  public Permission getPermissionById(UUID id) {
    return requirePermission(id);
  }

  @Override
  public Permission getPermissionByResourceAndAction(String resource, String action) {
    return permissionRepository
        .findByResourceAndAction(resource, action)
        .orElseThrow(() -> PermissionNotFoundException.byResourceAndAction(resource, action));
  }

  @Override
  public List<Permission> getAllPermissions() {
    return permissionRepository.findAll();
  }

  private Permission requirePermission(UUID id) {
    return permissionRepository
        .findById(id)
        .orElseThrow(() -> PermissionNotFoundException.byId(id));
  }

  private void requireUniquePair(String resource, String action) {
    if (permissionRepository.existsByResourceAndAction(resource, action)) {
      throw DuplicatePermissionException.of(resource, action);
    }
  }
}
