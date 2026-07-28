package com.gymapi.auth.component;

import com.gymapi.common.security.JwksFetcher;
import com.nimbusds.jose.jwk.JWKSet;
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcBuilderCustomizer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/**
 * Swaps exactly one link out of the production security wiring: the JWKS comes from a key pair
 * generated for the suite instead of over HTTP from {@code ms-ga-identifier}. Everything the
 * library enforces — RS256 pinning, {@code kid} selection, signature, {@code iss}/{@code aud},
 * expiry — runs the real code against real tokens.
 *
 * <p>Protected routes now require a verified token (ADR-0003), so every MockMvc request carries a
 * SUPER_ADMIN bearer token by default. A test that needs a different token — or none — sets its own
 * {@code Authorization} header, which replaces the default.
 */
@TestConfiguration(proxyBeanMethods = false)
public class ComponentTestSecurity {

  @Bean
  @Primary
  JwksFetcher componentTestJwksFetcher() {
    return () -> new JWKSet(TestTokens.KEY.toPublicJWK());
  }

  @Bean
  MockMvcBuilderCustomizer componentTestDefaultBearerToken() {
    String token = TestTokens.adminToken();
    return builder ->
        builder.defaultRequest(
            MockMvcRequestBuilders.get("/").header("Authorization", "Bearer " + token));
  }
}
