package com.gymapi.auth.adapter.in.web.advice;

import com.gymapi.auth.adapter.in.web.dto.generated.ErrorResponse;
import com.gymapi.auth.adapter.in.web.dto.generated.ValidationError;
import com.gymapi.auth.domain.exception.DomainException;
import com.gymapi.auth.domain.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Turns every exception that escapes a controller into the {@code ErrorResponse} envelope published
 * in {@code auth-api.yaml}.
 *
 * <p>Two rules keep this useful in production:
 *
 * <ul>
 *   <li>Client mistakes (4xx) are logged at WARN without a stack trace — they are not incidents.
 *       Unexpected failures (5xx) are logged at ERROR with the cause attached.
 *   <li>A 5xx message is never derived from the exception. Internal detail stays in the log, keyed
 *       by the correlation id the caller also receives.
 * </ul>
 */
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

  private static final String INTERNAL_ERROR_MESSAGE =
      "An unexpected error occurred. Quote the traceId when reporting this.";
  private static final int MAX_REJECTED_VALUE_LENGTH = 120;
  private static final Set<String> REDACTED_FIELDS = Set.of("password", "secret", "token");

  private final ErrorResponseFactory errorResponses;

  // --- Domain -------------------------------------------------------------

  /**
   * One handler for every business rule violation: {@link DomainException} carries the {@link
   * ErrorCode}, and the code's category decides the status.
   */
  @ExceptionHandler(DomainException.class)
  public ResponseEntity<ErrorResponse> handleDomainException(
      DomainException ex, HttpServletRequest request) {
    ErrorCode code = ex.errorCode();
    return respond(errorResponses.statusFor(code), code, ex.getMessage(), request, ex);
  }

  // --- Request binding and validation -------------------------------------

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(
      MethodArgumentNotValidException ex, HttpServletRequest request) {

    List<ValidationError> fieldErrors =
        ex.getBindingResult().getFieldErrors().stream()
            .map(GlobalExceptionHandler::toValidationError)
            .sorted(Comparator.comparing(ValidationError::getField))
            .toList();

    return respondWithFieldErrors(fieldErrors, request, ex);
  }

  /** Raised by {@code @Validated} constraints on path variables and query parameters. */
  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ErrorResponse> handleConstraintViolation(
      ConstraintViolationException ex, HttpServletRequest request) {

    Set<ConstraintViolation<?>> violations = ex.getConstraintViolations();
    List<ValidationError> fieldErrors =
        violations == null
            ? List.of()
            : violations.stream()
                .map(GlobalExceptionHandler::toValidationError)
                .sorted(Comparator.comparing(ValidationError::getField))
                .toList();

    return respondWithFieldErrors(fieldErrors, request, ex);
  }

  /** A path variable or query parameter that could not be converted, e.g. a non-UUID id. */
  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ErrorResponse> handleTypeMismatch(
      MethodArgumentTypeMismatchException ex, HttpServletRequest request) {

    String expectedType =
        ex.getRequiredType() == null
            ? "the expected type"
            : "a valid " + ex.getRequiredType().getSimpleName().toUpperCase(Locale.ROOT);
    String message = "Parameter '" + ex.getName() + "' must be " + expectedType;

    return respond(HttpStatus.BAD_REQUEST, ErrorCode.MALFORMED_REQUEST, message, request, ex);
  }

  @ExceptionHandler(MissingServletRequestParameterException.class)
  public ResponseEntity<ErrorResponse> handleMissingParameter(
      MissingServletRequestParameterException ex, HttpServletRequest request) {
    String message = "Required parameter '" + ex.getParameterName() + "' is missing";
    return respond(HttpStatus.BAD_REQUEST, ErrorCode.MALFORMED_REQUEST, message, request, ex);
  }

  /** Unparseable or absent request body. The parser detail is not echoed back to the caller. */
  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ErrorResponse> handleUnreadableBody(
      HttpMessageNotReadableException ex, HttpServletRequest request) {
    return respond(
        HttpStatus.BAD_REQUEST,
        ErrorCode.MALFORMED_REQUEST,
        "Request body is missing or is not valid JSON",
        request,
        ex);
  }

  // --- Protocol -----------------------------------------------------------

  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  public ResponseEntity<ErrorResponse> handleMethodNotSupported(
      HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
    String message =
        "Method " + ex.getMethod() + " is not supported for this endpoint" + supported(ex);
    return respond(
        HttpStatus.METHOD_NOT_ALLOWED, ErrorCode.METHOD_NOT_ALLOWED, message, request, ex);
  }

  @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
  public ResponseEntity<ErrorResponse> handleMediaTypeNotSupported(
      HttpMediaTypeNotSupportedException ex, HttpServletRequest request) {
    String message =
        "Content type '" + ex.getContentType() + "' is not supported. Supported: application/json";
    return respond(
        HttpStatus.UNSUPPORTED_MEDIA_TYPE, ErrorCode.UNSUPPORTED_MEDIA_TYPE, message, request, ex);
  }

  @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
  public ResponseEntity<ErrorResponse> handleNoHandlerFound(
      Exception ex, HttpServletRequest request) {
    String message = "No endpoint " + request.getMethod() + " " + request.getRequestURI();
    return respond(HttpStatus.NOT_FOUND, ErrorCode.ENDPOINT_NOT_FOUND, message, request, ex);
  }

  // --- Security -----------------------------------------------------------

  @ExceptionHandler(AuthenticationException.class)
  public ResponseEntity<ErrorResponse> handleAuthentication(
      AuthenticationException ex, HttpServletRequest request) {
    return respond(
        HttpStatus.UNAUTHORIZED,
        ErrorCode.UNAUTHENTICATED,
        "Authentication is required to access this resource",
        request,
        ex);
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ErrorResponse> handleAccessDenied(
      AccessDeniedException ex, HttpServletRequest request) {
    return respond(HttpStatus.FORBIDDEN, ErrorCode.ACCESS_DENIED, "Access is denied", request, ex);
  }

  // --- Persistence --------------------------------------------------------

  /**
   * A constraint the application did not check first — a race on a uniqueness check, or a foreign
   * key that vanished mid-transaction. The database message is withheld because it leaks schema.
   */
  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(
      DataIntegrityViolationException ex, HttpServletRequest request) {
    return respond(
        HttpStatus.CONFLICT,
        ErrorCode.DATA_INTEGRITY_VIOLATION,
        "The request conflicts with existing data",
        request,
        ex);
  }

  // --- Fallback -----------------------------------------------------------

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleUnexpectedException(
      Exception ex, HttpServletRequest request) {
    return respond(
        HttpStatus.INTERNAL_SERVER_ERROR,
        ErrorCode.INTERNAL_ERROR,
        INTERNAL_ERROR_MESSAGE,
        request,
        ex);
  }

  // --- Helpers ------------------------------------------------------------

  private ResponseEntity<ErrorResponse> respondWithFieldErrors(
      List<ValidationError> fieldErrors, HttpServletRequest request, Exception ex) {

    String message =
        "Request validation failed for "
            + fieldErrors.size()
            + (fieldErrors.size() == 1 ? " field" : " fields");
    ErrorResponse body =
        errorResponses.create(
            HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_FAILED, message, request, fieldErrors);
    logFailure(HttpStatus.BAD_REQUEST, body, ex);
    return ResponseEntity.badRequest().body(body);
  }

  private ResponseEntity<ErrorResponse> respond(
      HttpStatus status, ErrorCode code, String message, HttpServletRequest request, Exception ex) {

    ErrorResponse body = errorResponses.create(status, code, message, request);
    logFailure(status, body, ex);
    return ResponseEntity.status(status).body(body);
  }

  private void logFailure(HttpStatus status, ErrorResponse body, Exception ex) {
    if (status.is5xxServerError()) {
      log.error(
          "{} {} -> {} {}", body.getPath(), body.getCode(), status.value(), body.getMessage(), ex);
    } else {
      log.warn(
          "{} {} -> {} {} ({})",
          body.getPath(),
          body.getCode(),
          status.value(),
          body.getMessage(),
          ex.getClass().getSimpleName());
    }
  }

  private static String supported(HttpRequestMethodNotSupportedException ex) {
    return ex.getSupportedHttpMethods() == null || ex.getSupportedHttpMethods().isEmpty()
        ? ""
        : ". Supported: " + ex.getSupportedHttpMethods();
  }

  private static ValidationError toValidationError(FieldError error) {
    return validationError(error.getField(), error.getRejectedValue(), error.getDefaultMessage());
  }

  private static ValidationError toValidationError(ConstraintViolation<?> violation) {
    String path = violation.getPropertyPath() == null ? "" : violation.getPropertyPath().toString();
    // Strip the "method.argument" prefix Hibernate Validator adds for parameter constraints.
    String field = path.contains(".") ? path.substring(path.lastIndexOf('.') + 1) : path;
    return validationError(field, violation.getInvalidValue(), violation.getMessage());
  }

  private static ValidationError validationError(String field, Object rejected, String message) {
    ValidationError error = new ValidationError();
    error.setField(field);
    error.setRejectedValue(renderRejectedValue(field, rejected));
    error.setMessage(message == null ? "is invalid" : message);
    return error;
  }

  /** Echoing the input back helps clients, but not when it is a credential or a 10 KB blob. */
  private static String renderRejectedValue(String field, Object rejected) {
    if (rejected == null) {
      return null;
    }
    String lowerCaseField = field == null ? "" : field.toLowerCase(Locale.ROOT);
    if (REDACTED_FIELDS.stream().anyMatch(lowerCaseField::contains)) {
      return "[redacted]";
    }
    String rendered = rejected.toString();
    return rendered.length() > MAX_REJECTED_VALUE_LENGTH
        ? rendered.substring(0, MAX_REJECTED_VALUE_LENGTH) + "…"
        : rendered;
  }
}
