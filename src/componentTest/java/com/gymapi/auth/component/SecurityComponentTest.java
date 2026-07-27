package com.gymapi.auth.component;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * The service verifies every token itself (ADR-0003, security.md §2) — these tests drive the whole
 * chain over HTTP, so what is asserted is the wiring: the shared library's filter and verifier, the
 * 401 rendered by the entry point in the platform envelope, and the one documented no-JWT bootstrap
 * route.
 *
 * <p>Setting an {@code Authorization} header here replaces the suite's default SUPER_ADMIN token (a
 * non-Bearer scheme stands in for "no token", since MockMvc's default request would otherwise fill
 * the header back in).
 */
@DisplayName("In-service JWT verification, end to end")
class SecurityComponentTest extends ComponentTestBase {

  private static final String NO_TOKEN = "Basic c2VydmljZTpjcmVkZW50aWFs";

  @Test
  void aVerifiedRs256TokenIsAccepted() throws Exception {
    mockMvc
        .perform(get("/auth/roles").header("Authorization", "Bearer " + TestTokens.adminToken()))
        .andExpect(status().isOk());
  }

  @Test
  void aRequestWithoutATokenIs401InTheEnvelope() throws Exception {
    mockMvc
        .perform(get("/auth/roles").header("Authorization", NO_TOKEN))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"))
        .andExpect(jsonPath("$.status").value(401))
        .andExpect(jsonPath("$.traceId").exists());
  }

  @Test
  void mutationsAreProtectedToo() throws Exception {
    mockMvc
        .perform(
            post("/auth/roles")
                .header("Authorization", NO_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"name": "INTRUDER"}
                    """))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));

    // Nothing was created behind the 401.
    mockMvc
        .perform(get("/auth/roles"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));
  }

  @Test
  void aSignatureFromTheWrongKeyIsRejected() throws Exception {
    assertRejected(get("/auth/roles"), TestTokens.tokenSignedByAnImpostor());
  }

  @Test
  void aKidTheJwksDoesNotContainIsRejected() throws Exception {
    assertRejected(get("/auth/roles"), TestTokens.tokenWithUnknownKid());
  }

  @Test
  void anHs256TokenIsRejectedRegardlessOfItsClaims() throws Exception {
    assertRejected(get("/auth/roles"), TestTokens.hs256Token());
  }

  @Test
  void anExpiredTokenIsRejected() throws Exception {
    // An hour past exp — far beyond the 60 seconds of tolerated skew.
    String expired =
        TestTokens.token(
            claims -> claims.expirationTime(Date.from(Instant.now().minusSeconds(3600))));
    assertRejected(get("/auth/roles"), expired);
  }

  @Test
  void aTokenForAnotherAudienceIsRejected() throws Exception {
    assertRejected(get("/auth/roles"), TestTokens.token(claims -> claims.audience("not-gymapi")));
  }

  @Test
  void aTokenFromAnotherIssuerIsRejected() throws Exception {
    assertRejected(
        get("/auth/roles"), TestTokens.token(claims -> claims.issuer("not-ms-ga-identifier")));
  }

  /**
   * The bootstrap exception (security.md §6, ADR-0003 §5): ms-ga-identifier calls this while
   * minting a token, so it has none to present. It is guarded by NetworkPolicy and by having no
   * gateway route — not by a JWT requirement that would deadlock login.
   */
  @Test
  void theBootstrapRouteStaysReachableWithoutAToken() throws Exception {
    mockMvc
        .perform(
            get("/auth/users/{userId}/roles/with-permissions", UUID.randomUUID())
                .header("Authorization", NO_TOKEN))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.roles.length()").value(0))
        .andExpect(jsonPath("$.permissions.length()").value(0));
  }

  private void assertRejected(MockHttpServletRequestBuilder request, String token)
      throws Exception {
    mockMvc
        .perform(request.header("Authorization", "Bearer " + token))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
  }
}
