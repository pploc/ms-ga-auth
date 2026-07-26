package com.gymapi.auth.component;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gymapi.auth.events.AuthEvent;
import com.gymapi.auth.events.PermissionChangeType;
import com.gymapi.auth.events.PermissionChanged;
import com.gymapi.auth.events.RoleAssigned;
import com.gymapi.auth.events.RoleRevoked;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;

@DisplayName("User role assignment and event publication, end to end")
class UserRoleComponentTest extends ComponentTestBase {

  @Test
  void assignsRevokesAndReportsRoles() throws Exception {
    UUID userId = UUID.randomUUID();
    UUID roleId = createRole("MEMBER");

    mockMvc
        .perform(
            post("/auth/users/{userId}/roles", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"roleId": "%s"}
                    """
                        .formatted(roleId)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.message").value("Role assigned."))
        .andExpect(jsonPath("$.userId").value(userId.toString()))
        .andExpect(jsonPath("$.roleId").value(roleId.toString()));

    mockMvc
        .perform(get("/auth/users/{userId}/roles", userId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].roleId").value(roleId.toString()));

    mockMvc
        .perform(get("/auth/users/{userId}/roles/{roleId}/check", userId, roleId))
        .andExpect(status().isOk())
        .andExpect(content().string("true"));

    mockMvc
        .perform(delete("/auth/users/{userId}/roles/{roleId}", userId, roleId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("Role removed."));

    mockMvc
        .perform(get("/auth/users/{userId}/roles/{roleId}/check", userId, roleId))
        .andExpect(status().isOk())
        .andExpect(content().string("false"));
  }

  @Test
  void flattensRolesAndPermissionsForTheLoginPath() throws Exception {
    UUID userId = UUID.randomUUID();
    UUID member = createRole("MEMBER");
    UUID trainer = createRole("TRAINER");
    UUID read = createPermission("booking", "read");
    UUID create = createPermission("booking", "create");

    grantPermissions(member, read);
    // The shared permission must appear once, not twice.
    grantPermissions(trainer, read, create);
    assignRole(userId, member);
    assignRole(userId, trainer);

    mockMvc
        .perform(get("/auth/users/{userId}/roles/with-permissions", userId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.userId").value(userId.toString()))
        .andExpect(jsonPath("$.roles.length()").value(2))
        .andExpect(jsonPath("$.permissions.length()").value(2))
        .andExpect(jsonPath("$.permissions", org.hamcrest.Matchers.hasItem("booking:read")))
        .andExpect(jsonPath("$.permissions", org.hamcrest.Matchers.hasItem("booking:create")));
  }

  @Test
  void returnsEmptyListsForAUserWithNoAssignments() throws Exception {
    mockMvc
        .perform(get("/auth/users/{userId}/roles/with-permissions", UUID.randomUUID()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.roles.length()").value(0))
        .andExpect(jsonPath("$.permissions.length()").value(0));
  }

  @Test
  void rejectsAssigningTheSameRoleTwice() throws Exception {
    UUID userId = UUID.randomUUID();
    UUID roleId = createRole("MEMBER");
    assignRole(userId, roleId);

    mockMvc
        .perform(
            post("/auth/users/{userId}/roles", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"roleId": "%s"}
                    """
                        .formatted(roleId)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("USER_ROLE_ALREADY_ASSIGNED"));
  }

  @Test
  void rejectsAssigningARoleThatDoesNotExist() throws Exception {
    mockMvc
        .perform(
            post("/auth/users/{userId}/roles", UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"roleId": "%s"}
                    """
                        .formatted(UUID.randomUUID())))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("ROLE_NOT_FOUND"));
  }

  @Test
  void rejectsRevokingARoleTheUserDoesNotHold() throws Exception {
    mockMvc
        .perform(
            delete("/auth/users/{userId}/roles/{roleId}", UUID.randomUUID(), UUID.randomUUID()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("USER_ROLE_NOT_FOUND"));
  }

  @Test
  void publishesAnAssignmentEventKeyedByUserAndTaggedWithAnEventId() throws Exception {
    UUID userId = UUID.randomUUID();
    UUID roleId = createRole("MEMBER");

    assignRole(userId, roleId);

    ProducerRecord<String, AuthEvent> published = capturePublishedRecord();

    assertThat(published.topic()).isEqualTo("auth.events.test");
    // Keyed by the aggregate, so every event about this user stays on one partition and in order.
    assertThat(published.key()).isEqualTo(userId.toString());

    AuthEvent event = published.value();
    assertThat(event.getOccurredAt()).isNotNull();
    assertThat(event.getPayload()).isInstanceOf(RoleAssigned.class);

    RoleAssigned payload = (RoleAssigned) event.getPayload();
    assertThat(payload.getUserId()).isEqualTo(userId.toString());
    assertThat(payload.getRoleId()).isEqualTo(roleId.toString());

    // The eventId is what lets an at-least-once consumer discard a redelivery.
    assertThat(UUID.fromString(event.getEventId())).isNotNull();
    assertThat(headerValue(published, "eventId")).isEqualTo(event.getEventId());
    assertThat(headerValue(published, "eventType")).isEqualTo("RoleAssigned");
  }

  @Test
  void publishesARevocationEvent() throws Exception {
    UUID userId = UUID.randomUUID();
    UUID roleId = createRole("MEMBER");
    assignRole(userId, roleId);

    mockMvc
        .perform(delete("/auth/users/{userId}/roles/{roleId}", userId, roleId))
        .andExpect(status().isOk());

    ArgumentCaptor<ProducerRecord<String, AuthEvent>> captor = recordCaptor();
    verify(kafkaTemplate, times(2)).send(captor.capture());

    assertThat(captor.getAllValues().get(1).value().getPayload()).isInstanceOf(RoleRevoked.class);
  }

  @Test
  void publishesAPermissionChangeKeyedByRole() throws Exception {
    UUID roleId = createRole("TRAINER");
    UUID permissionId = createPermission("exercise", "create");

    grantPermissions(roleId, permissionId);

    ProducerRecord<String, AuthEvent> published = capturePublishedRecord();
    assertThat(published.key()).isEqualTo(roleId.toString());

    PermissionChanged payload = (PermissionChanged) published.value().getPayload();
    assertThat(payload.getRoleName()).isEqualTo("TRAINER");
    assertThat(payload.getChangeType()).isEqualTo(PermissionChangeType.PERMISSIONS_UPDATED);
  }

  /**
   * The at-least-once contract in one test: if the broker will not acknowledge the event, the
   * request fails with a retryable 503 and the assignment is not left behind in the database.
   */
  @Test
  void rollsTheAssignmentBackWhenTheBrokerWillNotAcknowledge() throws Exception {
    UUID userId = UUID.randomUUID();
    UUID roleId = createRole("MEMBER");
    brokerIsUnreachable();

    mockMvc
        .perform(
            post("/auth/users/{userId}/roles", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"roleId": "%s"}
                    """
                        .formatted(roleId)))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.code").value("EVENT_PUBLISH_FAILED"))
        .andExpect(jsonPath("$.traceId").exists());

    mockMvc
        .perform(get("/auth/users/{userId}/roles", userId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));
  }

  @Test
  void doesNotPublishAnythingWhenTheRequestIsRejectedBeforeTheChange() throws Exception {
    mockMvc
        .perform(
            post("/auth/users/{userId}/roles", UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"roleId": "%s"}
                    """
                        .formatted(UUID.randomUUID())))
        .andExpect(status().isNotFound());

    verify(kafkaTemplate, never()).send(any(ProducerRecord.class));
  }

  private ProducerRecord<String, AuthEvent> capturePublishedRecord() {
    ArgumentCaptor<ProducerRecord<String, AuthEvent>> captor = recordCaptor();
    verify(kafkaTemplate).send(captor.capture());
    return captor.getValue();
  }

  @SuppressWarnings("unchecked")
  private ArgumentCaptor<ProducerRecord<String, AuthEvent>> recordCaptor() {
    return ArgumentCaptor.forClass(ProducerRecord.class);
  }

  private static String headerValue(ProducerRecord<String, AuthEvent> record, String name) {
    return new String(record.headers().lastHeader(name).value(), StandardCharsets.UTF_8);
  }
}
