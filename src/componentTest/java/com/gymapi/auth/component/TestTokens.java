package com.gymapi.auth.component;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Mints tokens for the component suite. The signing key pair is generated once per JVM and its
 * private half never leaves the tests; {@link ComponentTestSecurity} serves the public half as the
 * JWKS, so the production verification path runs unmodified.
 */
final class TestTokens {

  static final String KID = "component-test-key";
  static final String ISSUER = "ms-ga-identifier";
  static final String AUDIENCE = "gymapi";

  /** The key identifier verifies against. */
  static final RSAKey KEY = generateKey(KID);

  /** A different key pair under the same kid — a signature that must not verify. */
  static final RSAKey IMPOSTOR_KEY = generateKey(KID);

  private TestTokens() {}

  /** A well-formed SUPER_ADMIN token: what an administrator would present on /auth routes. */
  static String adminToken() {
    return signedWith(KEY, claims(defaults -> {}));
  }

  /** An RS256 token signed with the suite's key; {@code customizer} overrides default claims. */
  static String token(Consumer<JWTClaimsSet.Builder> customizer) {
    return signedWith(KEY, claims(customizer));
  }

  /** Valid-looking claims, but signed by a key identifier never published. */
  static String tokenSignedByAnImpostor() {
    return signedWith(IMPOSTOR_KEY, claims(defaults -> {}));
  }

  /** Valid-looking claims under a kid the JWKS does not contain. */
  static String tokenWithUnknownKid() {
    return signedWith(generateKey("never-published-key"), claims(defaults -> {}));
  }

  /**
   * The HS256-confusion attack: a symmetric signature over valid-looking claims. The verifier pins
   * RS256, so this must be rejected before any key lookup happens.
   */
  static String hs256Token() {
    try {
      SignedJWT jwt =
          new SignedJWT(
              new JWSHeader.Builder(JWSAlgorithm.HS256).keyID(KID).build(), claims(c -> {}));
      jwt.sign(
          new MACSigner(
              "component-test-hs256-secret-that-is-definitely-long-enough"
                  .getBytes(StandardCharsets.UTF_8)));
      return jwt.serialize();
    } catch (JOSEException e) {
      throw new IllegalStateException("Could not mint the HS256 test token", e);
    }
  }

  private static JWTClaimsSet claims(Consumer<JWTClaimsSet.Builder> customizer) {
    Instant now = Instant.now();
    JWTClaimsSet.Builder builder =
        new JWTClaimsSet.Builder()
            .subject("00000000-0000-0000-0000-0000000000aa")
            .issuer(ISSUER)
            .audience(AUDIENCE)
            .jwtID(UUID.randomUUID().toString())
            .issueTime(Date.from(now))
            .expirationTime(Date.from(now.plusSeconds(900)))
            .claim("email", "component-test@gym.com")
            .claim("roles", List.of("SUPER_ADMIN"))
            .claim("permissions", List.of("role:manage"));
    customizer.accept(builder);
    return builder.build();
  }

  private static String signedWith(RSAKey key, JWTClaimsSet claims) {
    try {
      SignedJWT jwt =
          new SignedJWT(
              new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(key.getKeyID()).build(), claims);
      jwt.sign(new RSASSASigner(key));
      return jwt.serialize();
    } catch (JOSEException e) {
      throw new IllegalStateException("Could not mint an RS256 test token", e);
    }
  }

  private static RSAKey generateKey(String kid) {
    try {
      return new RSAKeyGenerator(2048).keyID(kid).generate();
    } catch (JOSEException e) {
      throw new IllegalStateException("Could not generate the test key pair", e);
    }
  }
}
