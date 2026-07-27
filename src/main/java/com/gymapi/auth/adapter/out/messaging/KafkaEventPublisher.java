package com.gymapi.auth.adapter.out.messaging;

import com.gymapi.auth.application.port.out.EventPublisher;
import com.gymapi.auth.config.properties.EventsProperties;
import com.gymapi.auth.domain.exception.EventPublishFailedException;
import com.gymapi.auth.events.AuthEvent;
import com.gymapi.auth.events.PermissionChangeType;
import com.gymapi.auth.events.PermissionChanged;
import com.gymapi.auth.events.RoleAssigned;
import com.gymapi.auth.events.RoleRevoked;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

/**
 * Publishes RBAC domain events as Avro, with at-least-once delivery.
 *
 * <p>The payload is a generated {@link AuthEvent} whose schema lives in {@code
 * api/avro/AuthEvent.avsc} — that file, not this class, is the contract with the analytic and
 * notification services. Each message carries the id of the schema it was written against, so an
 * incompatible change is refused by the registry at publish time instead of surfacing as a parse
 * error in a consumer days later.
 *
 * <p>Three things make the at-least-once guarantee real:
 *
 * <ul>
 *   <li><b>The send is confirmed.</b> The caller blocks until every in-sync replica has acked, and
 *       a failure throws — inside the caller's transaction, so the state change rolls back with it.
 *       A consumer may therefore see an event twice, but a committed change is never silently
 *       unannounced.
 *   <li><b>The producer retries.</b> {@code acks=all} plus an idempotent producer means the client
 *       replays transient failures itself and the broker discards the resulting duplicates.
 *   <li><b>Consumers can de-duplicate.</b> Every event carries a unique {@code eventId}, also set
 *       as a record header so a replay can be skipped without deserializing the body.
 * </ul>
 *
 * <p>Records are keyed by the aggregate the event is about — the user for assignment events, the
 * role for permission changes — so everything touching one entity lands on one partition and stays
 * ordered.
 *
 * <p>Residual gap: the broker ack and the database commit are still two separate commits. If the
 * process dies between them the event is out but the change is not. Closing that needs a
 * transactional outbox, which is a larger change than this class.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaEventPublisher implements EventPublisher {

  static final String HEADER_EVENT_ID = "eventId";
  static final String HEADER_EVENT_TYPE = "eventType";

  private final KafkaTemplate<String, AuthEvent> kafkaTemplate;
  private final EventsProperties properties;
  private final Clock clock;

  @Override
  public void publishRoleAssigned(String userId, String roleId, String assignedBy) {
    publish(
        userId,
        RoleAssigned.newBuilder()
            .setUserId(userId)
            .setRoleId(roleId)
            .setAssignedBy(assignedBy)
            .build());
  }

  @Override
  public void publishRoleRevoked(String userId, String roleId) {
    publish(userId, RoleRevoked.newBuilder().setUserId(userId).setRoleId(roleId).build());
  }

  @Override
  public void publishPermissionChanged(String roleId, String roleName, String changeType) {
    publish(
        roleId,
        PermissionChanged.newBuilder()
            .setRoleId(roleId)
            .setRoleName(roleName)
            .setChangeType(changeType(changeType))
            .build());
  }

  /**
   * The port speaks strings; the schema has a closed enum. A mismatch is a bug in this service, so
   * it fails with a message naming the fix rather than a bare {@code IllegalArgumentException}.
   */
  private static PermissionChangeType changeType(String value) {
    try {
      return PermissionChangeType.valueOf(value.toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException e) {
      throw new IllegalStateException(
          "Unknown permission change type '" + value + "'; add the symbol to AuthEvent.avsc", e);
    }
  }

  private void publish(String partitionKey, Object payload) {
    String eventId = UUID.randomUUID().toString();
    String eventType = payload.getClass().getSimpleName();

    AuthEvent event =
        AuthEvent.newBuilder()
            .setEventId(eventId)
            .setOccurredAt(clock.instant())
            .setPayload(payload)
            .build();

    ProducerRecord<String, AuthEvent> record =
        new ProducerRecord<>(properties.topic(), partitionKey, event);
    record.headers().add(header(HEADER_EVENT_ID, eventId));
    record.headers().add(header(HEADER_EVENT_TYPE, eventType));

    awaitAck(eventType, eventId, record);
  }

  private void awaitAck(
      String eventType, String eventId, ProducerRecord<String, AuthEvent> record) {
    long timeoutMs = properties.publishTimeout().toMillis();
    try {
      SendResult<String, AuthEvent> result =
          kafkaTemplate.send(record).get(timeoutMs, TimeUnit.MILLISECONDS);

      log.info(
          "Published {} eventId={} to {}-{}@{}",
          eventType,
          eventId,
          result.getRecordMetadata().topic(),
          result.getRecordMetadata().partition(),
          result.getRecordMetadata().offset());

    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw EventPublishFailedException.of(eventType, e);
    } catch (ExecutionException | TimeoutException e) {
      // Also the path for a schema the registry refuses: a serializer failure surfaces here.
      log.error("Broker did not ack {} eventId={} within {}ms", eventType, eventId, timeoutMs, e);
      throw EventPublishFailedException.of(eventType, e);
    }
  }

  private static RecordHeader header(String name, String value) {
    return new RecordHeader(name, value.getBytes(StandardCharsets.UTF_8));
  }
}
