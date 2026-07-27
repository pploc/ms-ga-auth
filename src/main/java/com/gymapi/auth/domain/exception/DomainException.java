package com.gymapi.auth.domain.exception;

/**
 * Base class for every business rule violation raised by this service.
 *
 * <p>Carrying an {@link ErrorCode} lets the web layer translate any domain failure into an HTTP
 * response with a single handler, instead of one {@code @ExceptionHandler} per exception type.
 */
public abstract class DomainException extends RuntimeException {

  private final transient ErrorCode errorCode;

  protected DomainException(ErrorCode errorCode, String message) {
    super(message);
    this.errorCode = errorCode;
  }

  protected DomainException(ErrorCode errorCode, String message, Throwable cause) {
    super(message, cause);
    this.errorCode = errorCode;
  }

  public ErrorCode errorCode() {
    return errorCode;
  }
}
