package com.gymapi.auth.domain.exception;

import java.util.UUID;

/** Raised when revoking or reading an assignment that the user does not hold. */
public class UserRoleNotFoundException extends DomainException {

  public UserRoleNotFoundException(String message) {
    super(ErrorCode.USER_ROLE_NOT_FOUND, message);
  }

  public static UserRoleNotFoundException of(UUID userId, UUID roleId) {
    return new UserRoleNotFoundException("User " + userId + " is not assigned role " + roleId);
  }
}
