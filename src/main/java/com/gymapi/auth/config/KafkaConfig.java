package com.gymapi.auth.config;

import com.gymapi.auth.config.properties.EventsProperties;
import com.gymapi.auth.events.AuthEvent;
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.TopicConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

/**
 * Producer and topic definition for the auth event stream.
 *
 * <p>Values are Avro, serialized through the Confluent Schema Registry: each payload is prefixed
 * with the id of the schema it was written against, so a consumer resolves the exact writer schema
 * instead of guessing, and the registry rejects an incompatible schema at publish time rather than
 * letting a consumer discover it at read time. That is the part JSON could not give us — the old
 * hand-built map had no contract at all beyond a comment.
 *
 * <p>The producer factory is declared here rather than left to Boot so the serializer, the registry
 * and the at-least-once settings are all visible in one place. Declaring it also switches off
 * Boot's own {@code KafkaTemplate}, which would otherwise be typed for strings.
 *
 * <p>Replication and {@code min.insync.replicas} are configurable because they are the other half
 * of {@code acks=all}: with a single in-sync replica, "all replicas acked" is one broker's page
 * cache and losing that broker still loses the event. Production should run 3 replicas with a
 * minimum of 2 in sync; the defaults are sized for the single-broker docker-compose stack.
 */
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(KafkaProperties.class)
public class KafkaConfig {

  private final EventsProperties properties;

  @Bean
  public ProducerFactory<String, AuthEvent> authEventProducerFactory(
      KafkaProperties kafkaProperties, SslBundles sslBundles) {

    // Start from the spring.kafka.producer block so acks, retries and delivery.timeout.ms stay
    // configured in one place, then pin the serializers this topic needs.
    Map<String, Object> config = kafkaProperties.buildProducerProperties(sslBundles);
    config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class);
    config.put(
        AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, properties.schemaRegistryUrl());
    config.put(
        AbstractKafkaSchemaSerDeConfig.AUTO_REGISTER_SCHEMAS, properties.autoRegisterSchemas());
    // Without this the generated Instant field has no conversion and serialization fails.
    config.put(KafkaAvroSerializerConfig.AVRO_USE_LOGICAL_TYPE_CONVERTERS_CONFIG, true);

    return new DefaultKafkaProducerFactory<>(config);
  }

  @Bean
  public KafkaTemplate<String, AuthEvent> authEventKafkaTemplate(
      ProducerFactory<String, AuthEvent> producerFactory) {
    return new KafkaTemplate<>(producerFactory);
  }

  @Bean
  public NewTopic authEventsTopic() {
    return TopicBuilder.name(properties.topic())
        .partitions(properties.partitions())
        .replicas(properties.replicas())
        .config(
            TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, String.valueOf(properties.minInsyncReplicas()))
        .build();
  }
}
