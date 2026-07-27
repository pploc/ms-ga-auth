package com.gymapi.auth.config.properties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Settings for the auth event stream, bound as a record so they are immutable, validated at
 * startup, and injectable without scattering {@code @Value} strings through the adapters.
 *
 * @param topic topic domain events are published to
 * @param partitions partition count; events are keyed by aggregate id, so this bounds how much
 *     per-entity ordering can be parallelised
 * @param replicas replication factor requested when the topic is created
 * @param minInsyncReplicas how many replicas must ack before {@code acks=all} is satisfied. Leaving
 *     this at 1 makes {@code acks=all} durable only against a broker restart, not a broker loss
 * @param publishTimeout how long a publisher waits for the ack. Must exceed the producer's {@code
 *     delivery.timeout.ms} so its retries finish before we give up
 * @param schemaRegistryUrl Confluent Schema Registry holding the {@code AuthEvent} schema. Payloads
 *     carry a schema id rather than the schema itself, so this must be reachable to publish
 * @param autoRegisterSchemas whether the producer may register a new schema version on the fly.
 *     Convenient locally; in production the schema should be published by CI so an incompatible
 *     change is caught in review rather than by whichever pod deploys first
 */
@Validated
@ConfigurationProperties("gymapi.events")
public record EventsProperties(
    @DefaultValue("auth.events") @NotBlank String topic,
    @DefaultValue("3") @Positive int partitions,
    @DefaultValue("1") @Positive short replicas,
    @DefaultValue("1") @Positive int minInsyncReplicas,
    @DefaultValue("25s") Duration publishTimeout,
    @DefaultValue("http://localhost:8081") @NotBlank String schemaRegistryUrl,
    @DefaultValue("true") boolean autoRegisterSchemas) {}
