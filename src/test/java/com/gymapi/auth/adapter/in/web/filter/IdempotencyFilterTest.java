package com.gymapi.auth.adapter.in.web.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gymapi.auth.adapter.in.web.advice.ErrorResponseFactory;
import com.gymapi.auth.application.port.in.IdempotencyUseCase;
import com.gymapi.auth.application.port.in.IdempotencyUseCase.Outcome;
import jakarta.servlet.FilterChain;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.util.StreamUtils;

@ExtendWith(MockitoExtension.class)
class IdempotencyFilterTest {

  private static final String KEY = "3f2504e0-4f89-11d3-9a0c-0305e82c3301";
  private static final String BODY = "{\"name\":\"FRONT_DESK\"}";

  @Mock private IdempotencyUseCase idempotency;

  private final ObjectMapper objectMapper =
      new ObjectMapper().registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

  private IdempotencyFilter filter() {
    return new IdempotencyFilter(idempotency, new ErrorResponseFactory(), objectMapper);
  }

  @Test
  void aSafeMethodIsNotGuardedEvenWithAKey() throws Exception {
    MockHttpServletRequest request = request("GET", "/auth/roles", BODY, KEY);

    filter().doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

    verifyNoInteractions(idempotency);
  }

  @Test
  void aMutatingRequestWithoutAKeyIsNotGuarded() throws Exception {
    MockHttpServletRequest request = request("POST", "/auth/roles", BODY, null);

    filter().doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

    verifyNoInteractions(idempotency);
  }

  @Test
  void aClaimedKeyLetsTheRequestRunAndRecordsTheResponse() throws Exception {
    given(idempotency.claim(eq(KEY), eq("POST"), eq("/auth/roles"), anyString()))
        .willReturn(new Outcome.Proceed());

    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain chain =
        (req, res) -> {
          // The handler must still see the body even though the filter already read it.
          assertThat(StreamUtils.copyToString(req.getInputStream(), StandardCharsets.UTF_8))
              .isEqualTo(BODY);
          ((jakarta.servlet.http.HttpServletResponse) res).setStatus(201);
          res.setContentType(MediaType.APPLICATION_JSON_VALUE);
          res.getWriter().write("{\"id\":\"created\"}");
        };

    filter().doFilter(request("POST", "/auth/roles", BODY, KEY), response, chain);

    assertThat(response.getStatus()).isEqualTo(201);
    assertThat(response.getContentAsString()).isEqualTo("{\"id\":\"created\"}");
    verify(idempotency).complete(eq(KEY), eq(201), anyString(), eq("{\"id\":\"created\"}"));
  }

  @Test
  void aStoredResponseIsReplayedWithoutRunningTheChain() throws Exception {
    given(idempotency.claim(any(), any(), any(), any()))
        .willReturn(new Outcome.Replay(201, "application/json", "{\"id\":\"original\"}"));

    MockHttpServletResponse response = new MockHttpServletResponse();
    MockFilterChain chain = new MockFilterChain();

    filter().doFilter(request("POST", "/auth/roles", BODY, KEY), response, chain);

    assertThat(response.getStatus()).isEqualTo(201);
    assertThat(response.getHeader(IdempotencyFilter.REPLAYED_HEADER)).isEqualTo("true");
    assertThat(response.getContentAsString()).isEqualTo("{\"id\":\"original\"}");
    assertThat(chain.getRequest()).isNull();
    verify(idempotency, never()).complete(any(), anyInt(), any(), any());
  }

  @Test
  void aReplayWithNoStoredBodyStillReturnsTheStatus() throws Exception {
    given(idempotency.claim(any(), any(), any(), any()))
        .willReturn(new Outcome.Replay(204, null, null));

    MockHttpServletResponse response = new MockHttpServletResponse();

    filter().doFilter(request("DELETE", "/auth/roles/7", "", KEY), response, new MockFilterChain());

    assertThat(response.getStatus()).isEqualTo(204);
    assertThat(response.getContentAsString()).isEmpty();
  }

  @Test
  void aReplayOfAnEmptyBodyWritesNothing() throws Exception {
    given(idempotency.claim(any(), any(), any(), any()))
        .willReturn(new Outcome.Replay(204, "application/json", ""));

    MockHttpServletResponse response = new MockHttpServletResponse();

    filter().doFilter(request("DELETE", "/auth/roles/7", "", KEY), response, new MockFilterChain());

    assertThat(response.getStatus()).isEqualTo(204);
    assertThat(response.getContentAsString()).isEmpty();
  }

  @Test
  void reusingAKeyForAnotherRequestIsRejected() throws Exception {
    given(idempotency.claim(any(), any(), any(), any()))
        .willReturn(new Outcome.KeyReused("POST", "/auth/permissions"));

    MockHttpServletResponse response = new MockHttpServletResponse();

    filter().doFilter(request("POST", "/auth/roles", BODY, KEY), response, new MockFilterChain());

    assertThat(response.getStatus()).isEqualTo(409);
    JsonNode body = objectMapper.readTree(response.getContentAsString());
    assertThat(body.get("code").asText()).isEqualTo("IDEMPOTENCY_KEY_REUSED");
    assertThat(body.get("message").asText()).contains("POST /auth/permissions");
  }

  @Test
  void aRetryWhileTheFirstAttemptIsRunningIsRejected() throws Exception {
    given(idempotency.claim(any(), any(), any(), any())).willReturn(new Outcome.InProgress());

    MockHttpServletResponse response = new MockHttpServletResponse();

    filter().doFilter(request("POST", "/auth/roles", BODY, KEY), response, new MockFilterChain());

    assertThat(response.getStatus()).isEqualTo(409);
    assertThat(objectMapper.readTree(response.getContentAsString()).get("code").asText())
        .isEqualTo("IDEMPOTENT_REQUEST_IN_PROGRESS");
  }

  @Test
  void aServerErrorReleasesTheClaimSoTheRetryRunsForReal() throws Exception {
    given(idempotency.claim(any(), any(), any(), any())).willReturn(new Outcome.Proceed());

    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain chain =
        (req, res) -> ((jakarta.servlet.http.HttpServletResponse) res).setStatus(503);

    filter().doFilter(request("POST", "/auth/roles", BODY, KEY), response, chain);

    verify(idempotency).abandon(KEY);
    verify(idempotency, never()).complete(any(), anyInt(), any(), any());
  }

  @Test
  void anExceptionEscapingTheChainAlsoReleasesTheClaim() {
    given(idempotency.claim(any(), any(), any(), any())).willReturn(new Outcome.Proceed());

    FilterChain exploding =
        (req, res) -> {
          throw new IllegalStateException("boom");
        };

    assertThatThrownBy(
            () ->
                filter()
                    .doFilter(
                        request("POST", "/auth/roles", BODY, KEY),
                        new MockHttpServletResponse(),
                        exploding))
        .isInstanceOf(IllegalStateException.class);

    verify(idempotency).abandon(KEY);
  }

  @Test
  void aKeyWithWhitespaceIsRejected() throws Exception {
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter()
        .doFilter(
            request("POST", "/auth/roles", BODY, "has a space"), response, new MockFilterChain());

    assertThat(response.getStatus()).isEqualTo(400);
    assertThat(objectMapper.readTree(response.getContentAsString()).get("code").asText())
        .isEqualTo("MALFORMED_REQUEST");
    verifyNoInteractions(idempotency);
  }

  @Test
  void anEmptyKeyIsRejected() throws Exception {
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter().doFilter(request("POST", "/auth/roles", BODY, "   "), response, new MockFilterChain());

    assertThat(response.getStatus()).isEqualTo(400);
    verifyNoInteractions(idempotency);
  }

  @Test
  void anOverlongKeyIsRejected() throws Exception {
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter()
        .doFilter(
            request("POST", "/auth/roles", BODY, "k".repeat(256)), response, new MockFilterChain());

    assertThat(response.getStatus()).isEqualTo(400);
    verifyNoInteractions(idempotency);
  }

  /** The same body under a different path must not look like the same request. */
  @Test
  void theFingerprintCoversMethodPathAndBody() throws Exception {
    given(idempotency.claim(any(), any(), any(), any())).willReturn(new Outcome.Proceed());
    ArgumentCaptor<String> fingerprints = ArgumentCaptor.forClass(String.class);

    filter()
        .doFilter(request("POST", "/auth/roles", BODY, KEY), new MockHttpServletResponse(), noop());
    filter()
        .doFilter(
            request("POST", "/auth/permissions", BODY, KEY), new MockHttpServletResponse(), noop());
    filter()
        .doFilter(
            request("POST", "/auth/roles", "{\"name\":\"OTHER\"}", KEY),
            new MockHttpServletResponse(),
            noop());

    verify(idempotency, org.mockito.Mockito.times(3))
        .claim(any(), any(), any(), fingerprints.capture());
    assertThat(fingerprints.getAllValues()).doesNotHaveDuplicates();
    assertThat(fingerprints.getAllValues()).allMatch(f -> f.matches("[0-9a-f]{64}"));
  }

  private static FilterChain noop() {
    return (req, res) -> {};
  }

  private static MockHttpServletRequest request(
      String method, String path, String body, String key) {
    MockHttpServletRequest request = new MockHttpServletRequest(method, path);
    request.setContent(body.getBytes(StandardCharsets.UTF_8));
    request.setContentType(MediaType.APPLICATION_JSON_VALUE);
    if (key != null) {
      request.addHeader(IdempotencyFilter.HEADER_NAME, key);
    }
    return request;
  }
}
