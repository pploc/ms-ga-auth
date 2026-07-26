package com.gymapi.auth.domain.exception;

import java.util.UUID;

/** Raised when a permission is looked up by an identifier that matches nothing. */
public class PermissionNotFoundException extends DomainException {

  public PermissionNotFoundException(String message) {
    super(ErrorCode.PERMISSION_NOT_FOUND, message);
  }

  public static PermissionNotFoundException byId(UUID id) {
    return new PermissionNotFoundException("Permission not found with id: " + id);
  }

  public static PermissionNotFoundException byResourceAndAction(String resource, String action) {
    return new PermissionNotFoundException(
        "Permission not found with resource: " + resource + " and action: " + action);
  }
}
