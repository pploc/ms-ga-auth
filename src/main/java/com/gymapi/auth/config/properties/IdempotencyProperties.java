package com.gymapi.auth.config.properties;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Settings for {@code Idempotency-Key} replay.
 *
 * @param retention how long a completed response stays replayable. Long enough to cover a client's
 *     whole retry schedule, short enough that the table does not grow without bound
 * @param inProgressTimeout after this, a claim whose request never reported back is treated as
 *     abandoned and may be taken over. Without it a crash mid-request would block the key for the
 *     entire retention window
 * @param purgeInterval how often expired records are deleted
 */
@Validated
@ConfigurationProperties("gymapi.idempotency")
public record IdempotencyProperties(
    @DefaultValue("24h") Duration retention,
    @DefaultValue("60s") Duration inProgressTimeout,
    @DefaultValue("1h") Duration purgeInterval) {}
