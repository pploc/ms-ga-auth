package com.gymapi.auth.adapter.out.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import com.gymapi.auth.events.AuthEvent;
import com.gymapi.auth.events.PermissionChangeType;
import com.gymapi.auth.events.PermissionChanged;
import com.gymapi.auth.events.RoleAssigned;
import com.gymapi.auth.events.RoleRevoked;
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig;
import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumWriter;
import org.junit.jupiter.api.Test;

/**
 * Exercises the wire format itself.
 *
 * <p>The unit tests around the publisher stop at the {@code KafkaTemplate} boundary, so nothing
 * there would notice a record the serializer cannot actually write — a logical type without a
 * converter, say. These tests serialize for real against an in-memory Schema Registry (the {@code
 * mock://} URL scheme), which is where such a mismatch shows up.
 */
class AuthEventSchemaTest {

  private static final String TOPIC = "auth.events";

  private static final Map<String, Object> SERDE_CONFIG =
      Map.of(
          AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG,
              "mock://auth-event-schema-test",
          KafkaAvroSerializerConfig.AVRO_USE_LOGICAL_TYPE_CONVERTERS_CONFIG, true,
          KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, true);

  @Test
  void aRoleAssignedEventSurvivesTheWire() {
    AuthEvent original =
        event(
            RoleAssigned.newBuilder()
                .setUserId("550e8400-e29b-41d4-a716-446655440000")
                .setRoleId("7d3f1a90-1c2b-4e55-8f31-9a0b6c4d2e11")
                .setAssignedBy("1f0c7d22-4b6e-4a91-8c3d-0e5f2a7b9c14")
                .build());

    AuthEvent decoded = roundTrip(original);

    assertThat(decoded).isEqualTo(original);
    assertThat(decoded.getOccurredAt()).isEqualTo(original.getOccurredAt());
    assertThat(decoded.getPayload()).isInstanceOf(RoleAssigned.class);
  }

  @Test
  void anOptionalFieldSurvivesAsNull() {
    AuthEvent original =
        event(
            RoleAssigned.newBuilder()
                .setUserId("550e8400-e29b-41d4-a716-446655440000")
                .setRoleId("7d3f1a90-1c2b-4e55-8f31-9a0b6c4d2e11")
                .setAssignedBy(null)
                .build());

    assertThat(((RoleAssigned) roundTrip(original).getPayload()).getAssignedBy()).isNull();
  }

  @Test
  void eachUnionBranchIsResolvedBackToItsOwnType() {
    assertThat(
            roundTrip(event(RoleRevoked.newBuilder().setUserId("u").setRoleId("r").build()))
                .getPayload())
        .isInstanceOf(RoleRevoked.class);

    AuthEvent changed =
        event(
            PermissionChanged.newBuilder()
                .setRoleId("r")
                .setRoleName("TRAINER")
                .setChangeType(PermissionChangeType.PERMISSIONS_UPDATED)
                .build());
    PermissionChanged decoded = (PermissionChanged) roundTrip(changed).getPayload();
    assertThat(decoded.getChangeType()).isEqualTo(PermissionChangeType.PERMISSIONS_UPDATED);
  }

  /**
   * The point of a registry: a consumer pinned to an older reader schema keeps working. Here the
   * reader only knows the envelope — exactly what an analytics consumer that counts events but
   * ignores their content would use — and Avro resolves the difference rather than failing.
   */
  @Test
  void aConsumerOnAnOlderReaderSchemaCanStillReadTodaysEvents() throws Exception {
    Schema envelopeOnly =
        new Schema.Parser()
            .parse(
                """
                {
                  "namespace": "com.gymapi.auth.events",
                  "type": "record",
                  "name": "AuthEvent",
                  "fields": [
                    {"name": "eventId", "type": "string"},
                    {"name": "occurredAt", "type": {"type": "long", "logicalType": "timestamp-millis"}}
                  ]
                }
                """);

    AuthEvent written =
        event(RoleAssigned.newBuilder().setUserId("u").setRoleId("r").setAssignedBy(null).build());

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    DatumWriter<AuthEvent> writer = new SpecificDatumWriter<>(AuthEvent.getClassSchema());
    var encoder = EncoderFactory.get().binaryEncoder(out, null);
    writer.write(written, encoder);
    encoder.flush();

    GenericRecord read =
        new GenericDatumReader<GenericRecord>(AuthEvent.getClassSchema(), envelopeOnly)
            .read(null, DecoderFactory.get().binaryDecoder(out.toByteArray(), null));

    assertThat(read.get("eventId")).hasToString(written.getEventId());
    assertThat(read.hasField("payload")).isFalse();
  }

  private static AuthEvent event(Object payload) {
    return AuthEvent.newBuilder()
        .setEventId(UUID.randomUUID().toString())
        .setOccurredAt(Instant.parse("2026-07-26T10:00:00Z"))
        .setPayload(payload)
        .build();
  }

  private static AuthEvent roundTrip(AuthEvent event) {
    try (KafkaAvroSerializer serializer = new KafkaAvroSerializer();
        KafkaAvroDeserializer deserializer = new KafkaAvroDeserializer()) {

      serializer.configure(SERDE_CONFIG, false);
      deserializer.configure(SERDE_CONFIG, false);

      byte[] wire = serializer.serialize(TOPIC, event);
      // Confluent wire format: a magic byte, then the four-byte schema id, then the Avro body.
      assertThat(wire[0]).isZero();
      assertThat(wire.length).isGreaterThan(5);

      return (AuthEvent) deserializer.deserialize(TOPIC, wire);
    }
  }
}
