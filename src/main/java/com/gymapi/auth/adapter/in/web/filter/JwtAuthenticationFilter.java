package com.gymapi.auth.adapter.in.web.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.crypto.SecretKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Populates the security context from the bearer token issued by {@code ms-ga-identifier}.
 *
 * <p>Roles are read straight from the token's {@code roles} claim — the fat-JWT model means this
 * service never calls back to identity to authorize a request.
 *
 * <p>A missing or unusable token is not rejected here; the filter simply leaves the request
 * anonymous and lets the authorization rules in {@code SecurityConfig} decide, so that public
 * endpoints keep working.
 *
 * <p>Deliberately not a {@code @Component}: Spring Boot auto-registers {@code Filter} beans into
 * the servlet chain, which would run this a second time outside the security chain. {@code
 * SecurityConfig} constructs it instead.
 */
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private static final String BEARER_PREFIX = "Bearer ";

  private final SecretKey signingKey;

  public JwtAuthenticationFilter(String jwtSecret) {
    this.signingKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    String token = bearerToken(request);
    if (token != null) {
      authenticate(token);
    }
    filterChain.doFilter(request, response);
  }

  private void authenticate(String token) {
    try {
      Claims claims =
          Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token).getPayload();

      String userId = claims.getSubject();
      if (userId == null) {
        return;
      }

      @SuppressWarnings("unchecked")
      List<String> roles = claims.get("roles", List.class);
      List<SimpleGrantedAuthority> authorities =
          roles == null ? List.of() : roles.stream().map(SimpleGrantedAuthority::new).toList();

      SecurityContextHolder.getContext()
          .setAuthentication(new UsernamePasswordAuthenticationToken(userId, null, authorities));

    } catch (JwtException | IllegalArgumentException e) {
      // Expected for expired or tampered tokens: stay anonymous and let authorization decide.
      log.debug("Rejected bearer token: {}", e.getMessage());
      SecurityContextHolder.clearContext();
    }
  }

  private static String bearerToken(HttpServletRequest request) {
    String header = request.getHeader("Authorization");
    return header != null && header.startsWith(BEARER_PREFIX)
        ? header.substring(BEARER_PREFIX.length())
        : null;
  }
}
