package com.gymapi.auth.adapter.in.web.advice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.gymapi.auth.adapter.in.web.dto.generated.ErrorResponse;
import com.gymapi.auth.adapter.in.web.dto.generated.ValidationError;
import com.gymapi.auth.adapter.in.web.filter.CorrelationIdFilter;
import com.gymapi.auth.domain.exception.ErrorCode;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;

class ErrorResponseFactoryTest {

  private final ErrorResponseFactory factory = new ErrorResponseFactory();

  @AfterEach
  void clearMdc() {
    MDC.clear();
  }

  /**
   * The wire enum is generated from auth-api.yaml. If a code is added to the domain without being
   * published in the spec, clients would receive a code the contract never mentions — so fail the
   * build here rather than degrade at runtime.
   */
  @Test
  void everyDomainErrorCodeIsPublishedInTheOpenApiContract() {
    Set<String> published =
        EnumSet.allOf(ErrorResponse.CodeEnum.class).stream()
            .map(ErrorResponse.CodeEnum::getValue)
            .collect(Collectors.toSet());
    Set<String> domain =
        EnumSet.allOf(ErrorCode.class).stream().map(Enum::name).collect(Collectors.toSet());

    assertEquals(domain, published, "ErrorCode and auth-api.yaml ErrorResponse.code have drifted");
  }

  @ParameterizedTest
  @EnumSource(ErrorCode.class)
  void everyCodeMapsToANonSuccessStatus(ErrorCode code) {
    HttpStatus status = factory.statusFor(code);

    assertNotNull(status);
    assertTrue(status.isError(), code + " mapped to " + status);
  }

  @Test
  void categoriesMapToTheDocumentedStatuses() {
    assertEquals(HttpStatus.NOT_FOUND, factory.statusFor(ErrorCode.ROLE_NOT_FOUND));
    assertEquals(HttpStatus.CONFLICT, factory.statusFor(ErrorCode.ROLE_ALREADY_EXISTS));
    assertEquals(HttpStatus.FORBIDDEN, factory.statusFor(ErrorCode.SYSTEM_ROLE_IMMUTABLE));
    assertEquals(HttpStatus.BAD_REQUEST, factory.statusFor(ErrorCode.VALIDATION_FAILED));
    assertEquals(HttpStatus.UNAUTHORIZED, factory.statusFor(ErrorCode.UNAUTHENTICATED));
    assertEquals(HttpStatus.METHOD_NOT_ALLOWED, factory.statusFor(ErrorCode.METHOD_NOT_ALLOWED));
    assertEquals(
        HttpStatus.UNSUPPORTED_MEDIA_TYPE, factory.statusFor(ErrorCode.UNSUPPORTED_MEDIA_TYPE));
    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, factory.statusFor(ErrorCode.INTERNAL_ERROR));
  }

  @Test
  void bodyCarriesTheRequestPathAndTheCurrentCorrelationId() {
    MDC.put(CorrelationIdFilter.MDC_KEY, "trace-123");
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/auth/roles/7");

    ErrorResponse body = factory.create(ErrorCode.ROLE_NOT_FOUND, "nope", request);

    assertEquals(404, body.getStatus());
    assertEquals("Not Found", body.getError());
    assertEquals(ErrorResponse.CodeEnum.ROLE_NOT_FOUND, body.getCode());
    assertEquals("nope", body.getMessage());
    assertEquals("/auth/roles/7", body.getPath());
    assertEquals("trace-123", body.getTraceId());
    assertNotNull(body.getTimestamp());
  }

  @Test
  void emptyFieldErrorsAreLeftOutRatherThanSerialisedAsAnEmptyArray() {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/auth/roles");

    ErrorResponse withNone =
        factory.create(
            HttpStatus.BAD_REQUEST, ErrorCode.MALFORMED_REQUEST, "bad", request, List.of());
    assertNull(withNone.getFieldErrors());

    ValidationError violation = new ValidationError();
    violation.setField("name");
    ErrorResponse withSome =
        factory.create(
            HttpStatus.BAD_REQUEST,
            ErrorCode.VALIDATION_FAILED,
            "bad",
            request,
            List.of(violation));
    assertEquals(1, withSome.getFieldErrors().size());
  }
}
