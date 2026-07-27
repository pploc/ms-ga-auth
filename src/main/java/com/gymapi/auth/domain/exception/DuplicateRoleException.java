package com.gymapi.auth.domain.exception;

/** Raised when a role name would collide with an existing role. */
public class DuplicateRoleException extends DomainException {

  public DuplicateRoleException(String message) {
    super(ErrorCode.ROLE_ALREADY_EXISTS, message);
  }

  public static DuplicateRoleException byName(String name) {
    return new DuplicateRoleException("Role with name '" + name + "' already exists");
  }
}
