package com.gymapi.auth.config;

import com.gymapi.auth.adapter.in.web.advice.RestAccessDeniedHandler;
import com.gymapi.auth.adapter.in.web.advice.RestAuthenticationEntryPoint;
import com.gymapi.auth.adapter.in.web.filter.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Stateless security for a service that sits behind the Kong gateway.
 *
 * <p>Kong validates the JWT signature, expiry and JTI blacklist before forwarding, and enforces
 * per-route permissions from the token claims, so the rules here are a second line of defence
 * rather than the primary gate.
 *
 * <p>The 401/403 handlers are wired explicitly so failures inside the filter chain produce the same
 * {@code ErrorResponse} envelope as failures inside a controller.
 */
@Configuration
@EnableWebSecurity
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

  private final String jwtSecret;

  public SecurityConfig(@Value("${jwt.secret}") String jwtSecret) {
    this.jwtSecret = jwtSecret;
  }

  @Bean
  public SecurityFilterChain securityFilterChain(
      HttpSecurity http,
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
                    .requestMatchers("/auth/**")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .exceptionHandling(
            handling ->
                handling
                    .authenticationEntryPoint(authenticationEntryPoint)
                    .accessDeniedHandler(accessDeniedHandler))
        .addFilterBefore(
            new JwtAuthenticationFilter(jwtSecret), UsernamePasswordAuthenticationFilter.class)
        .build();
  }
}
