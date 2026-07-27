package com.gymapi.auth.adapter.in.web.advice;

import com.gymapi.auth.adapter.in.web.dto.generated.ErrorResponse;
import com.gymapi.auth.adapter.in.web.dto.generated.ValidationError;
import com.gymapi.auth.adapter.in.web.filter.CorrelationIdFilter;
import com.gymapi.auth.domain.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Builds the one error envelope this service returns, so the exception advice and the Spring
 * Security entry points cannot drift apart.
 */
@Slf4j
@Component
public class ErrorResponseFactory {

  /** Maps a transport-agnostic domain category onto the HTTP status the API documents. */
  public HttpStatus statusFor(ErrorCode code) {
    return switch (code.category()) {
      case VALIDATION -> HttpStatus.BAD_REQUEST;
      case UNAUTHORIZED -> HttpStatus.UNAUTHORIZED;
      case FORBIDDEN -> HttpStatus.FORBIDDEN;
      case NOT_FOUND -> HttpStatus.NOT_FOUND;
      case CONFLICT -> HttpStatus.CONFLICT;
      case UNSUPPORTED -> unsupportedStatus(code);
      case UNAVAILABLE -> HttpStatus.SERVICE_UNAVAILABLE;
      case INTERNAL -> HttpStatus.INTERNAL_SERVER_ERROR;
    };
  }

  public ErrorResponse create(ErrorCode code, String message, HttpServletRequest request) {
    return create(statusFor(code), code, message, request, null);
  }

  public ErrorResponse create(
      HttpStatus status, ErrorCode code, String message, HttpServletRequest request) {
    return create(status, code, message, request, null);
  }

  public ErrorResponse create(
      HttpStatus status,
      ErrorCode code,
      String message,
      HttpServletRequest request,
      List<ValidationError> fieldErrors) {

    ErrorResponse body = new ErrorResponse();
    body.setTimestamp(OffsetDateTime.now(ZoneOffset.UTC));
    body.setStatus(status.value());
    body.setError(status.getReasonPhrase());
    body.setCode(toWireCode(code));
    body.setMessage(message);
    body.setPath(request == null ? null : request.getRequestURI());
    body.setTraceId(CorrelationIdFilter.currentCorrelationId());
    // Left null rather than empty so the field is omitted for non-validation failures
    // (Jackson is configured to drop nulls).
    body.setFieldErrors(fieldErrors == null || fieldErrors.isEmpty() ? null : fieldErrors);
    return body;
  }

  /**
   * The wire enum is generated from the OpenAPI spec, so a code added to {@link ErrorCode} without
   * being published in the spec would blow up here. {@code ErrorCodeContractTest} fails the build
   * on that drift; this fallback keeps a live service from turning it into a 500 storm.
   */
  private ErrorResponse.CodeEnum toWireCode(ErrorCode code) {
    try {
      return ErrorResponse.CodeEnum.fromValue(code.name());
    } catch (IllegalArgumentException e) {
      log.error(
          "ErrorCode {} is missing from components.schemas.ErrorResponse.code in auth-api.yaml",
          code);
      return ErrorResponse.CodeEnum.INTERNAL_ERROR;
    }
  }

  private static HttpStatus unsupportedStatus(ErrorCode code) {
    return code == ErrorCode.UNSUPPORTED_MEDIA_TYPE
        ? HttpStatus.UNSUPPORTED_MEDIA_TYPE
        : HttpStatus.METHOD_NOT_ALLOWED;
  }
}
