package com.gymapi.auth.domain.exception;

/**
 * Raised when a caller tries to mutate a role flagged as a system role.
 *
 * <p>System roles (SUPER_ADMIN, GYM_ADMIN, TRAINER, MEMBER, STAFF) are seeded by migration and
 * every service in the platform relies on their names and permission sets, so they are read-only
 * through the API.
 */
public class SystemRoleModificationException extends DomainException {

  public SystemRoleModificationException(String message) {
    super(ErrorCode.SYSTEM_ROLE_IMMUTABLE, message);
  }

  public static SystemRoleModificationException cannotUpdate(String roleName) {
    return new SystemRoleModificationException("System role '" + roleName + "' cannot be updated");
  }

  public static SystemRoleModificationException cannotDelete(String roleName) {
    return new SystemRoleModificationException("System role '" + roleName + "' cannot be deleted");
  }

  public static SystemRoleModificationException cannotChangePermissions(String roleName) {
    return new SystemRoleModificationException(
        "Permissions of system role '" + roleName + "' cannot be changed");
  }
}
