package com.gymapi.auth.adapter.in.web.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Gives every request a correlation id, exposed three ways: in the log MDC (the console pattern in
 * {@code application.yml} prints {@code %X{correlationId}}), on the response as {@code
 * X-Correlation-Id}, and as {@code traceId} in error bodies.
 *
 * <p>An inbound id is reused so a trace survives across services, but it is sanitised first —
 * echoing an arbitrary caller-supplied string into logs invites log forging.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

  public static final String HEADER_NAME = "X-Correlation-Id";
  public static final String MDC_KEY = "correlationId";

  private static final int MAX_LENGTH = 64;

  /** Returns the correlation id of the request being handled, or {@code null} outside a request. */
  public static String currentCorrelationId() {
    return MDC.get(MDC_KEY);
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    String correlationId = sanitise(request.getHeader(HEADER_NAME));
    MDC.put(MDC_KEY, correlationId);
    response.setHeader(HEADER_NAME, correlationId);
    try {
      filterChain.doFilter(request, response);
    } finally {
      MDC.remove(MDC_KEY);
    }
  }

  private static String sanitise(String inbound) {
    if (inbound == null || inbound.isBlank()) {
      return UUID.randomUUID().toString();
    }
    String cleaned = inbound.replaceAll("[^A-Za-z0-9_.\\-]", "");
    if (cleaned.isEmpty()) {
      return UUID.randomUUID().toString();
    }
    return cleaned.length() > MAX_LENGTH ? cleaned.substring(0, MAX_LENGTH) : cleaned;
  }
}
