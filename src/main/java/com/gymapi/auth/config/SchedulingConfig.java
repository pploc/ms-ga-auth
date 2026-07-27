package com.gymapi.auth.config;

import com.gymapi.auth.adapter.in.scheduler.IdempotencyCleanupJob;
import com.gymapi.auth.config.properties.IdempotencyProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

/**
 * Registers the background work.
 *
 * <p>Kept apart from {@link ApplicationConfig} on purpose: this class depends on the cleanup job,
 * which depends transitively on the {@code Clock} bean {@code ApplicationConfig} declares. Holding
 * both in one configuration class is a bean cycle.
 */
@Configuration
@EnableScheduling
@RequiredArgsConstructor
public class SchedulingConfig implements SchedulingConfigurer {

  private final IdempotencyProperties idempotencyProperties;
  private final IdempotencyCleanupJob idempotencyCleanupJob;

  /**
   * Registered here rather than with {@code @Scheduled}: the annotation's {@code fixedDelayString}
   * only parses milliseconds or ISO-8601, so it cannot take the {@code Duration} the rest of the
   * configuration is expressed in.
   */
  @Override
  public void configureTasks(ScheduledTaskRegistrar registrar) {
    registrar.addFixedDelayTask(
        idempotencyCleanupJob::purgeExpiredRecords, idempotencyProperties.purgeInterval());
  }
}
