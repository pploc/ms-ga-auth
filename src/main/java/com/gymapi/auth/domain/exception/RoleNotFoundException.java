package com.gymapi.auth.domain.exception;

import java.util.UUID;

/** Raised when a role is looked up by an identifier that matches nothing. */
public class RoleNotFoundException extends DomainException {

  public RoleNotFoundException(String message) {
    super(ErrorCode.ROLE_NOT_FOUND, message);
  }

  public static RoleNotFoundException byId(UUID id) {
    return new RoleNotFoundException("Role not found with id: " + id);
  }

  public static RoleNotFoundException byName(String name) {
    return new RoleNotFoundException("Role not found with name: " + name);
  }
}
