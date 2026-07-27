package com.gymapi.auth.adapter.in.web.advice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gymapi.auth.adapter.in.web.dto.generated.ErrorResponse;
import com.gymapi.auth.domain.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/**
 * Renders 403s raised inside the security filter chain in the same envelope as every other error.
 */
@Component
@RequiredArgsConstructor
public class RestAccessDeniedHandler implements AccessDeniedHandler {

  private final ErrorResponseFactory errorResponses;
  private final ObjectMapper objectMapper;

  @Override
  public void handle(
      HttpServletRequest request,
      HttpServletResponse response,
      AccessDeniedException accessDeniedException)
      throws IOException {

    ErrorResponse body =
        errorResponses.create(
            HttpStatus.FORBIDDEN, ErrorCode.ACCESS_DENIED, "Access is denied", request);

    response.setStatus(HttpStatus.FORBIDDEN.value());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    objectMapper.writeValue(response.getOutputStream(), body);
  }
}
