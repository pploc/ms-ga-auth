package com.gymapi.auth.adapter.in.web.advice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.gymapi.auth.adapter.in.web.dto.generated.ErrorResponse;
import com.gymapi.auth.domain.exception.ErrorCode;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Defensive branches that a request cannot realistically reach through MockMvc, exercised by
 * calling the handler directly. They exist so a surprising input produces a normal error response
 * instead of a second exception inside the error path.
 */
class GlobalExceptionHandlerEdgeCasesTest {

  private final GlobalExceptionHandler handler =
      new GlobalExceptionHandler(new ErrorResponseFactory());
  private final MockHttpServletRequest request = new MockHttpServletRequest("POST", "/auth/roles");

  @Test
  void aTypeMismatchWithNoDeclaredTypeStillProducesAMessage() {
    MethodArgumentTypeMismatchException exception =
        new MethodArgumentTypeMismatchException("x", null, "id", mock(MethodParameter.class), null);

    ResponseEntity<ErrorResponse> response = handler.handleTypeMismatch(exception, request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody().getMessage())
        .isEqualTo("Parameter 'id' must be the expected type");
  }

  @Test
  void anUnsupportedMethodWithNoAlternativesOmitsTheSupportedList() {
    HttpRequestMethodNotSupportedException exception =
        new HttpRequestMethodNotSupportedException("TRACE");

    ResponseEntity<ErrorResponse> response = handler.handleMethodNotSupported(exception, request);

    assertThat(response.getBody().getMessage())
        .isEqualTo("Method TRACE is not supported for this endpoint");
  }

  @Test
  void anUnsupportedMethodListsTheAlternativesWhenThereAreSome() {
    HttpRequestMethodNotSupportedException exception =
        new HttpRequestMethodNotSupportedException("PATCH", List.of("GET", "POST"));

    ResponseEntity<ErrorResponse> response = handler.handleMethodNotSupported(exception, request);

    assertThat(response.getBody().getMessage()).contains("Supported: [GET, POST]");
  }

  @Test
  void aConstraintViolationExceptionWithNoViolationsIsStillAValidationFailure() {
    ResponseEntity<ErrorResponse> response =
        handler.handleConstraintViolation(new ConstraintViolationException(null), request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody().getCode()).isEqualTo(ErrorResponse.CodeEnum.VALIDATION_FAILED);
    assertThat(response.getBody().getMessage()).isEqualTo("Request validation failed for 0 fields");
    assertThat(response.getBody().getFieldErrors()).isNull();
  }

  @Test
  void aFieldErrorWithoutAMessageFallsBackToAGenericOne() {
    MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
    BindingResult bindingResult = mock(BindingResult.class);
    given(exception.getBindingResult()).willReturn(bindingResult);
    given(bindingResult.getFieldErrors())
        .willReturn(List.of(new FieldError("payload", "name", null, false, null, null, null)));

    ResponseEntity<ErrorResponse> response =
        handler.handleMethodArgumentNotValid(exception, request);

    assertThat(response.getBody().getFieldErrors()).hasSize(1);
    assertThat(response.getBody().getFieldErrors().get(0).getMessage()).isEqualTo("is invalid");
    // A null submitted value is reported as absent rather than as the string "null".
    assertThat(response.getBody().getFieldErrors().get(0).getRejectedValue()).isNull();
  }

  @Test
  void anErrorBodyCanBeBuiltWithoutARequest() {
    ErrorResponse body =
        new ErrorResponseFactory()
            .create(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_ERROR, "boom", null);

    assertThat(body.getPath()).isNull();
    assertThat(body.getStatus()).isEqualTo(500);
  }

  @Test
  void everyHttpMethodConstantIsCoveredByTheUnsupportedMapping() {
    // METHOD_NOT_ALLOWED and UNSUPPORTED_MEDIA_TYPE share the UNSUPPORTED category and must not
    // collapse onto the same status.
    ErrorResponseFactory factory = new ErrorResponseFactory();

    assertThat(factory.statusFor(ErrorCode.METHOD_NOT_ALLOWED))
        .isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
    assertThat(factory.statusFor(ErrorCode.UNSUPPORTED_MEDIA_TYPE))
        .isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    assertThat(factory.statusFor(ErrorCode.EVENT_PUBLISH_FAILED))
        .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
  }
}
