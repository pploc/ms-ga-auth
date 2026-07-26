package com.gymapi.auth.domain.exception;

/** Raised when a {@code resource:action} pair would collide with an existing permission. */
public class DuplicatePermissionException extends DomainException {

  public DuplicatePermissionException(String message) {
    super(ErrorCode.PERMISSION_ALREADY_EXISTS, message);
  }

  public static DuplicatePermissionException of(String resource, String action) {
    return new DuplicatePermissionException(
        "Permission with resource '" + resource + "' and action '" + action + "' already exists");
  }
}
