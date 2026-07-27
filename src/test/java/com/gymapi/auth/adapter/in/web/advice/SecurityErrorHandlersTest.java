package com.gymapi.auth.adapter.in.web.advice;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gymapi.auth.adapter.in.web.filter.CorrelationIdFilter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;

/**
 * 401s and 403s raised inside the security filter chain never reach the
 * {@code @RestControllerAdvice}, so these handlers exist to stop the API from having two error
 * shapes.
 */
class SecurityErrorHandlersTest {

  private final ObjectMapper objectMapper =
      new ObjectMapper().registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
  private final ErrorResponseFactory factory = new ErrorResponseFactory();

  @AfterEach
  void clearMdc() {
    MDC.clear();
  }

  @Test
  void theEntryPointRendersTheStandardEnvelopeFor401() throws Exception {
    MDC.put(CorrelationIdFilter.MDC_KEY, "trace-401");
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/auth/roles");
    MockHttpServletResponse response = new MockHttpServletResponse();

    new RestAuthenticationEntryPoint(factory, objectMapper)
        .commence(request, response, new BadCredentialsException("bad token"));

    assertThat(response.getStatus()).isEqualTo(401);
    assertThat(response.getContentType()).isEqualTo("application/json");

    JsonNode body = objectMapper.readTree(response.getContentAsString());
    assertThat(body.get("code").asText()).isEqualTo("UNAUTHENTICATED");
    assertThat(body.get("error").asText()).isEqualTo("Unauthorized");
    assertThat(body.get("path").asText()).isEqualTo("/auth/roles");
    assertThat(body.get("traceId").asText()).isEqualTo("trace-401");
  }

  @Test
  void theAccessDeniedHandlerRendersTheStandardEnvelopeFor403() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("DELETE", "/auth/roles/7");
    MockHttpServletResponse response = new MockHttpServletResponse();

    new RestAccessDeniedHandler(factory, objectMapper)
        .handle(request, response, new AccessDeniedException("nope"));

    assertThat(response.getStatus()).isEqualTo(403);

    JsonNode body = objectMapper.readTree(response.getContentAsString());
    assertThat(body.get("code").asText()).isEqualTo("ACCESS_DENIED");
    assertThat(body.get("message").asText()).isEqualTo("Access is denied");
    assertThat(body.get("path").asText()).isEqualTo("/auth/roles/7");
  }
}
