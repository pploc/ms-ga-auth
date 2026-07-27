package com.gymapi.auth.config;

import com.gymapi.auth.adapter.in.web.advice.RestAccessDeniedHandler;
import com.gymapi.auth.adapter.in.web.advice.RestAuthenticationEntryPoint;
import com.gymapi.common.security.CachingJwksKeySource;
import com.gymapi.common.security.HttpJwksFetcher;
import com.gymapi.common.security.JwksFetcher;
import com.gymapi.common.security.JwtAuthenticationFilter;
import com.gymapi.common.security.JwtProperties;
import com.gymapi.common.security.JwtVerifier;
import java.time.Clock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Stateless security with independent JWT verification (security.md §2, ADR-0003).
 *
 * <p>The gateway verifies tokens too, but this service no longer relies on that: every protected
 * request is verified in-process by the shared library's {@link JwtVerifier} — RS256 pinned, key
 * selected by {@code kid} from the JWKS {@code ms-ga-identifier} publishes, {@code iss} and {@code
 * aud gymapi} checked, 60 seconds of clock skew. No shared signing secret exists here any more; the
 * service holds nothing but cached public keys.
 *
 * <p>The one exception is the bootstrap route, {@code GET
 * /auth/users/{userId}/roles/with-permissions}: {@code ms-ga-identifier} calls it <em>while minting
 * a token</em>, so the caller has none to present and requiring one would deadlock login. It stays
 * open at this layer, authenticated by service credential and restricted by the NetworkPolicy to
 * identifier's pods, and is published through no gateway route at all (security.md §6, ADR-0003
 * §5).
 *
 * <p>The 401/403 handlers are wired explicitly so failures inside the filter chain produce the same
 * {@code ErrorResponse} envelope as failures inside a controller.
 */
@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

  /** Paths that must stay reachable without a token: probes and the API docs. */
  private static final String[] PUBLIC_PATHS = {
    "/actuator/health",
    "/actuator/health/**",
    "/actuator/info",
    "/v3/api-docs",
    "/v3/api-docs/**",
    "/swagger-ui.html",
    "/swagger-ui/**"
  };

  /** The documented no-JWT bootstrap route — see the class comment. */
  private static final String BOOTSTRAP_PATH = "/auth/users/*/roles/with-permissions";

  @Bean
  public JwksFetcher jwksFetcher(JwtProperties properties) {
    return new HttpJwksFetcher(properties.jwksUri());
  }

  @Bean
  public JwtVerifier jwtVerifier(JwtProperties properties, JwksFetcher jwksFetcher, Clock clock) {
    return new JwtVerifier(
        properties, new CachingJwksKeySource(jwksFetcher, properties, clock), clock);
  }

  @Bean
  public SecurityFilterChain securityFilterChain(
      HttpSecurity http,
      JwtVerifier jwtVerifier,
      RestAuthenticationEntryPoint authenticationEntryPoint,
      RestAccessDeniedHandler accessDeniedHandler)
      throws Exception {

    return http.csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(PUBLIC_PATHS)
                    .permitAll()
                    // CORS preflights carry no Authorization header by definition.
                    .requestMatchers(HttpMethod.OPTIONS, "/**")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, BOOTSTRAP_PATH)
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .exceptionHandling(
            handling ->
                handling
                    .authenticationEntryPoint(authenticationEntryPoint)
                    .accessDeniedHandler(accessDeniedHandler))
        .addFilterBefore(
            new JwtAuthenticationFilter(jwtVerifier), UsernamePasswordAuthenticationFilter.class)
        .build();
  }
}
