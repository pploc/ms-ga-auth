package com.gymapi.auth.adapter.in.web.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class JwtAuthenticationFilterTest {

  private static final String SECRET = "unit-test-secret-key-long-enough-for-hmac-sha256-signing";
  private static final SecretKey KEY = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));

  private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(SECRET);

  @AfterEach
  void clearContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void populatesTheContextFromTheTokensSubjectAndRoles() throws Exception {
    String token =
        signedToken("user-1", List.of("MEMBER", "TRAINER"), Instant.now().plusSeconds(60));

    authenticate(token);

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    assertThat(authentication).isNotNull();
    assertThat(authentication.getPrincipal()).isEqualTo("user-1");
    assertThat(authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority))
        .containsExactly("MEMBER", "TRAINER");
  }

  @Test
  void aTokenWithoutRolesAuthenticatesWithNoAuthorities() throws Exception {
    authenticate(signedToken("user-2", null, Instant.now().plusSeconds(60)));

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    assertThat(authentication).isNotNull();
    assertThat(authentication.getAuthorities()).isEmpty();
  }

  @Test
  void aTokenWithoutASubjectIsIgnored() throws Exception {
    authenticate(signedToken(null, List.of("MEMBER"), Instant.now().plusSeconds(60)));

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
  }

  /** Expired or tampered tokens leave the request anonymous; authorization decides from there. */
  @Test
  void anExpiredTokenLeavesTheRequestAnonymous() throws Exception {
    authenticate(signedToken("user-3", List.of("MEMBER"), Instant.now().minusSeconds(60)));

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
  }

  @Test
  void aTokenSignedWithAnotherKeyIsRejected() throws Exception {
    SecretKey otherKey =
        Keys.hmacShaKeyFor(
            "a-completely-different-secret-of-sufficient-length!!"
                .getBytes(StandardCharsets.UTF_8));
    String forged =
        Jwts.builder()
            .subject("attacker")
            .expiration(Date.from(Instant.now().plusSeconds(60)))
            .signWith(otherKey)
            .compact();

    authenticate(forged);

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
  }

  @Test
  void gibberishInTheHeaderIsRejectedWithoutBlowingUp() throws Exception {
    authenticate("not-a-jwt");

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
  }

  @Test
  void aRequestWithNoAuthorizationHeaderPassesStraightThrough() throws Exception {
    MockFilterChain chain = new MockFilterChain();

    filter.doFilter(
        new MockHttpServletRequest("GET", "/auth/roles"), new MockHttpServletResponse(), chain);

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    assertThat(chain.getRequest()).isNotNull();
  }

  @Test
  void aNonBearerAuthorizationSchemeIsIgnored() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/auth/roles");
    request.addHeader("Authorization", "Basic dXNlcjpwYXNz");

    filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
  }

  @Test
  void theChainAlwaysContinues() throws Exception {
    jakarta.servlet.FilterChain chain = org.mockito.Mockito.mock(jakarta.servlet.FilterChain.class);
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/auth/roles");
    request.addHeader("Authorization", "Bearer nonsense");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, chain);

    verify(chain).doFilter(request, response);
  }

  private void authenticate(String token) throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/auth/roles");
    request.addHeader("Authorization", "Bearer " + token);
    filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());
  }

  private static String signedToken(String subject, List<String> roles, Instant expiry) {
    var builder = Jwts.builder().expiration(Date.from(expiry)).signWith(KEY);
    if (subject != null) {
      builder.subject(subject);
    }
    if (roles != null) {
      builder.claim("roles", roles);
    }
    return builder.compact();
  }
}
