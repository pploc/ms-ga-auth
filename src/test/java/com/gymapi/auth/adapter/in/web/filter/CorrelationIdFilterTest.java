package com.gymapi.auth.adapter.in.web.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.servlet.FilterChain;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class CorrelationIdFilterTest {

  private final CorrelationIdFilter filter = new CorrelationIdFilter();

  @Test
  void generatesAnIdWhenTheCallerSendsNone() throws Exception {
    MockHttpServletResponse response = run(new MockHttpServletRequest("GET", "/auth/roles"));

    String header = response.getHeader(CorrelationIdFilter.HEADER_NAME);
    assertNotNull(header);
    assertNotNull(UUID.fromString(header));
  }

  @Test
  void reusesTheInboundIdSoTracesSpanServices() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/auth/roles");
    request.addHeader(CorrelationIdFilter.HEADER_NAME, "upstream-1234");

    MockHttpServletResponse response = run(request);

    assertEquals("upstream-1234", response.getHeader(CorrelationIdFilter.HEADER_NAME));
  }

  @Test
  void stripsCharactersThatCouldForgeALogLine() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/auth/roles");
    request.addHeader(CorrelationIdFilter.HEADER_NAME, "ab\ncd\"level\":\"ERROR\"");

    MockHttpServletResponse response = run(request);

    assertEquals("abcdlevelERROR", response.getHeader(CorrelationIdFilter.HEADER_NAME));
  }

  @Test
  void truncatesAnOverlongInboundId() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/auth/roles");
    request.addHeader(CorrelationIdFilter.HEADER_NAME, "x".repeat(500));

    MockHttpServletResponse response = run(request);

    assertEquals(64, response.getHeader(CorrelationIdFilter.HEADER_NAME).length());
  }

  @Test
  void fallsBackToAGeneratedIdWhenTheInboundValueSanitisesToNothing() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/auth/roles");
    request.addHeader(CorrelationIdFilter.HEADER_NAME, "!!!");

    MockHttpServletResponse response = run(request);

    assertNotNull(UUID.fromString(response.getHeader(CorrelationIdFilter.HEADER_NAME)));
  }

  @Test
  void exposesTheIdOnTheMdcDuringTheRequestAndClearsItAfterwards() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/auth/roles");
    request.addHeader(CorrelationIdFilter.HEADER_NAME, "during-request");

    FilterChain chain =
        (req, res) -> assertEquals("during-request", CorrelationIdFilter.currentCorrelationId());

    filter.doFilter(request, new MockHttpServletResponse(), chain);

    assertNull(MDC.get(CorrelationIdFilter.MDC_KEY));
  }

  @Test
  void clearsTheMdcEvenWhenTheChainBlowsUp() {
    FilterChain exploding =
        (req, res) -> {
          throw new IllegalStateException("boom");
        };

    assertTrue(
        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalStateException.class,
                () ->
                    filter.doFilter(
                        new MockHttpServletRequest("GET", "/auth/roles"),
                        new MockHttpServletResponse(),
                        exploding))
            .getMessage()
            .equals("boom"));
    assertNull(MDC.get(CorrelationIdFilter.MDC_KEY));
  }

  private MockHttpServletResponse run(MockHttpServletRequest request) throws Exception {
    MockHttpServletResponse response = new MockHttpServletResponse();
    filter.doFilter(request, response, new MockFilterChain());
    return response;
  }
}
