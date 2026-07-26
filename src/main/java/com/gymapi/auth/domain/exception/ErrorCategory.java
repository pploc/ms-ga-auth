package com.gymapi.auth.domain.exception;

/**
 * Transport-agnostic classification of an {@link ErrorCode}.
 *
 * <p>The domain deliberately does not know about HTTP. Inbound adapters translate a category into
 * whatever their protocol uses (see the web advice for the HTTP status mapping).
 */
public enum ErrorCategory {

  /** The request was syntactically or semantically invalid. */
  VALIDATION,

  /** The caller is not authenticated. */
  UNAUTHORIZED,

  /** The caller is authenticated but the operation is not permitted. */
  FORBIDDEN,

  /** The addressed resource does not exist. */
  NOT_FOUND,

  /** The request conflicts with the current state of the resource. */
  CONFLICT,

  /** The request used an operation the resource does not support. */
  UNSUPPORTED,

  /** A dependency is temporarily unavailable; the same request is worth retrying. */
  UNAVAILABLE,

  /** Something failed that the caller cannot act on. */
  INTERNAL
}
