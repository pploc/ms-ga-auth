package com.gymapi.auth.domain.exception;

import java.util.UUID;

/** Raised when assigning a role the user already holds. */
public class DuplicateUserRoleException extends DomainException {

  public DuplicateUserRoleException(String message) {
    super(ErrorCode.USER_ROLE_ALREADY_ASSIGNED, message);
  }

  public static DuplicateUserRoleException of(UUID userId, UUID roleId) {
    return new DuplicateUserRoleException("User " + userId + " already has role " + roleId);
  }
}
