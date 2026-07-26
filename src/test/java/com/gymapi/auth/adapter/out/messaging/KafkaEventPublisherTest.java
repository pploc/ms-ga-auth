package com.gymapi.auth.adapter.out.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.gymapi.auth.config.properties.EventsProperties;
import com.gymapi.auth.domain.exception.ErrorCode;
import com.gymapi.auth.domain.exception.EventPublishFailedException;
import com.gymapi.auth.events.AuthEvent;
import com.gymapi.auth.events.PermissionChangeType;
import com.gymapi.auth.events.PermissionChanged;
import com.gymapi.auth.events.RoleAssigned;
import com.gymapi.auth.events.RoleRevoked;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

@ExtendWith(MockitoExtension.class)
class KafkaEventPublisherTest {

  private static final String TOPIC = "auth-test-topic";
  private static final Instant NOW = Instant.parse("2026-07-26T10:00:00Z");

  @Mock private KafkaTemplate<String, AuthEvent> kafkaTemplate;

  private KafkaEventPublisher publisher;

  @BeforeEach
  void setUp() {
    publisher =
        new KafkaEventPublisher(kafkaTemplate, properties(200), Clock.fixed(NOW, ZoneOffset.UTC));
  }

  @Test
  void roleAssignedIsKeyedByUserAndCarriesADeduplicationId() {
    String userId = UUID.randomUUID().toString();
    String roleId = UUID.randomUUID().toString();
    String assignedBy = UUID.randomUUID().toString();
    brokerAcks();

    publisher.publishRoleAssigned(userId, roleId, assignedBy);

    ProducerRecord<String, AuthEvent> sent = captureSent();
    assertThat(sent.topic()).isEqualTo(TOPIC);
    // Keyed by aggregate: everything about one user stays ordered on one partition.
    assertThat(sent.key()).isEqualTo(userId);

    AuthEvent event = sent.value();
    assertThat(UUID.fromString(event.getEventId())).isNotNull();
    assertThat(event.getOccurredAt()).isEqualTo(NOW);

    // The union branch is the event type; there is no separate string to fall out of step.
    assertThat(event.getPayload()).isInstanceOf(RoleAssigned.class);
    RoleAssigned payload = (RoleAssigned) event.getPayload();
    assertThat(payload.getUserId()).isEqualTo(userId);
    assertThat(payload.getRoleId()).isEqualTo(roleId);
    assertThat(payload.getAssignedBy()).isEqualTo(assignedBy);

    // Duplicated into headers so an at-least-once consumer can skip a replay without decoding.
    assertThat(header(sent, KafkaEventPublisher.HEADER_EVENT_ID)).isEqualTo(event.getEventId());
    assertThat(header(sent, KafkaEventPublisher.HEADER_EVENT_TYPE)).isEqualTo("RoleAssigned");
  }

  @Test
  void anAbsentActorIsPublishedAsNull() {
    brokerAcks();

    publisher.publishRoleAssigned(UUID.randomUUID().toString(), UUID.randomUUID().toString(), null);

    RoleAssigned payload = (RoleAssigned) captureSent().value().getPayload();
    assertThat(payload.getAssignedBy()).isNull();
  }

  @Test
  void roleRevokedIsKeyedByUser() {
    String userId = UUID.randomUUID().toString();
    String roleId = UUID.randomUUID().toString();
    brokerAcks();

    publisher.publishRoleRevoked(userId, roleId);

    ProducerRecord<String, AuthEvent> sent = captureSent();
    assertThat(sent.key()).isEqualTo(userId);
    assertThat(sent.value().getPayload()).isInstanceOf(RoleRevoked.class);
    assertThat(((RoleRevoked) sent.value().getPayload()).getRoleId()).isEqualTo(roleId);
    assertThat(header(sent, KafkaEventPublisher.HEADER_EVENT_TYPE)).isEqualTo("RoleRevoked");
  }

  @Test
  void permissionChangedIsKeyedByRole() {
    String roleId = UUID.randomUUID().toString();
    brokerAcks();

    publisher.publishPermissionChanged(roleId, "ADMIN", "permissions_updated");

    ProducerRecord<String, AuthEvent> sent = captureSent();
    assertThat(sent.key()).isEqualTo(roleId);

    PermissionChanged payload = (PermissionChanged) sent.value().getPayload();
    assertThat(payload.getRoleName()).isEqualTo("ADMIN");
    assertThat(payload.getChangeType()).isEqualTo(PermissionChangeType.PERMISSIONS_UPDATED);
  }

  /** The schema's enum is closed, so an unmapped value is a bug here rather than a bad message. */
  @Test
  void anUnknownChangeTypePointsAtTheSchema() {
    assertThatThrownBy(
            () -> publisher.publishPermissionChanged(UUID.randomUUID().toString(), "ADMIN", "nope"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("AuthEvent.avsc");
  }

  /**
   * The at-least-once contract: a send that is never acked has to fail loudly so the caller's
   * transaction rolls back with it. A schema the registry refuses arrives on this same path.
   */
  @Test
  void aRejectedSendFailsTheCallerRatherThanBeingSwallowed() {
    given(kafkaTemplate.send(any(ProducerRecord.class)))
        .willReturn(
            CompletableFuture.failedFuture(new IllegalStateException("no in-sync replicas")));

    assertThatThrownBy(
            () ->
                publisher.publishRoleRevoked(
                    UUID.randomUUID().toString(), UUID.randomUUID().toString()))
        .isInstanceOf(EventPublishFailedException.class)
        .hasMessageContaining("RoleRevoked")
        .extracting(e -> ((EventPublishFailedException) e).errorCode())
        .isEqualTo(ErrorCode.EVENT_PUBLISH_FAILED);
  }

  @Test
  void anAckThatNeverArrivesTimesOutAsAPublishFailure() {
    // A future that never completes: the producer used its whole retry budget and gave up.
    given(kafkaTemplate.send(any(ProducerRecord.class))).willReturn(new CompletableFuture<>());

    assertThatThrownBy(
            () ->
                publisher.publishPermissionChanged(
                    UUID.randomUUID().toString(), "ADMIN", "permissions_updated"))
        .isInstanceOf(EventPublishFailedException.class);
  }

  /** Shutting the service down mid-publish must not look like a successful publish. */
  @Test
  void anInterruptWhileWaitingIsAPublishFailureAndKeepsTheInterruptFlag() {
    given(kafkaTemplate.send(any(ProducerRecord.class))).willReturn(new CompletableFuture<>());
    Thread.currentThread().interrupt();
    try {
      assertThatThrownBy(
              () ->
                  publisher.publishRoleRevoked(
                      UUID.randomUUID().toString(), UUID.randomUUID().toString()))
          .isInstanceOf(EventPublishFailedException.class);
      assertThat(Thread.currentThread().isInterrupted()).isTrue();
    } finally {
      Thread.interrupted();
    }
  }

  private static EventsProperties properties(long publishTimeoutMs) {
    return new EventsProperties(
        TOPIC, 3, (short) 1, 1, Duration.ofMillis(publishTimeoutMs), "mock://unit-test", true);
  }

  private void brokerAcks() {
    given(kafkaTemplate.send(any(ProducerRecord.class)))
        .willAnswer(
            invocation -> {
              ProducerRecord<String, AuthEvent> record = invocation.getArgument(0);
              RecordMetadata metadata =
                  new RecordMetadata(new TopicPartition(record.topic(), 0), 0L, 0, 0L, 0, 0);
              return CompletableFuture.completedFuture(new SendResult<>(record, metadata));
            });
  }

  @SuppressWarnings("unchecked")
  private ProducerRecord<String, AuthEvent> captureSent() {
    ArgumentCaptor<ProducerRecord<String, AuthEvent>> captor =
        ArgumentCaptor.forClass(ProducerRecord.class);
    verify(kafkaTemplate).send(captor.capture());
    return captor.getValue();
  }

  private static String header(ProducerRecord<String, AuthEvent> record, String name) {
    return new String(record.headers().lastHeader(name).value(), StandardCharsets.UTF_8);
  }
}
