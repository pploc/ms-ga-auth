package com.gymapi.auth.domain.exception;

/**
 * Stable, machine-readable identifier returned in every error payload as {@code error.code}.
 *
 * <p>Clients are expected to branch on these values rather than on the human-readable message or on
 * the HTTP status alone. Constants are therefore part of the public API contract: add new ones
 * freely, but never rename or repurpose an existing one. The same list is published in {@code
 * api/openapi/auth-api.yaml} under {@code components.schemas.ErrorCode}.
 */
public enum ErrorCode {

  // --- Role ---------------------------------------------------------------
  ROLE_NOT_FOUND(ErrorCategory.NOT_FOUND),
  ROLE_ALREADY_EXISTS(ErrorCategory.CONFLICT),
  SYSTEM_ROLE_IMMUTABLE(ErrorCategory.FORBIDDEN),

  // --- Permission ---------------------------------------------------------
  PERMISSION_NOT_FOUND(ErrorCategory.NOT_FOUND),
  PERMISSION_ALREADY_EXISTS(ErrorCategory.CONFLICT),

  // --- User role assignment ----------------------------------------------
  USER_ROLE_NOT_FOUND(ErrorCategory.NOT_FOUND),
  USER_ROLE_ALREADY_ASSIGNED(ErrorCategory.CONFLICT),

  // --- Request handling ---------------------------------------------------
  VALIDATION_FAILED(ErrorCategory.VALIDATION),
  MALFORMED_REQUEST(ErrorCategory.VALIDATION),
  ENDPOINT_NOT_FOUND(ErrorCategory.NOT_FOUND),
  METHOD_NOT_ALLOWED(ErrorCategory.UNSUPPORTED),
  UNSUPPORTED_MEDIA_TYPE(ErrorCategory.UNSUPPORTED),

  // --- Idempotency --------------------------------------------------------
  IDEMPOTENCY_KEY_REUSED(ErrorCategory.CONFLICT),
  IDEMPOTENT_REQUEST_IN_PROGRESS(ErrorCategory.CONFLICT),

  // --- Security -----------------------------------------------------------
  UNAUTHENTICATED(ErrorCategory.UNAUTHORIZED),
  ACCESS_DENIED(ErrorCategory.FORBIDDEN),

  // --- Fallback -----------------------------------------------------------
  DATA_INTEGRITY_VIOLATION(ErrorCategory.CONFLICT),
  EVENT_PUBLISH_FAILED(ErrorCategory.UNAVAILABLE),
  INTERNAL_ERROR(ErrorCategory.INTERNAL);

  private final ErrorCategory category;

  ErrorCode(ErrorCategory category) {
    this.category = category;
  }

  public ErrorCategory category() {
    return category;
  }
}
