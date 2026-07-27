package com.gymapi.auth.config;

import com.gymapi.auth.config.properties.EventsProperties;
import com.gymapi.auth.config.properties.IdempotencyProperties;
import java.time.Clock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cross-cutting beans and property bindings.
 *
 * <p>Exposing {@link Clock} as a bean rather than calling {@code OffsetDateTime.now()} inline keeps
 * the time-sensitive parts of idempotency — expiry, abandoned-claim takeover — testable without
 * sleeping.
 */
@Configuration
@EnableConfigurationProperties({EventsProperties.class, IdempotencyProperties.class})
public class ApplicationConfig {

  @Bean
  public Clock clock() {
    return Clock.systemUTC();
  }
}
