package com.gymapi.auth.component;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gymapi.auth.adapter.out.persistence.repository.IdempotencyJpaRepository;
import com.gymapi.auth.adapter.out.persistence.repository.PermissionJpaRepository;
import com.gymapi.auth.adapter.out.persistence.repository.RoleJpaRepository;
import com.gymapi.auth.adapter.out.persistence.repository.RolePermissionJpaRepository;
import com.gymapi.auth.adapter.out.persistence.repository.UserRoleJpaRepository;
import com.gymapi.auth.events.AuthEvent;
import com.jayway.jsonpath.JsonPath;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Boots the whole application once per suite and drives it over HTTP.
 *
 * <p>Unlike the {@code @WebMvcTest} unit tests, nothing between the socket and the database is
 * stubbed: requests pass through the correlation-id filter, the security chain, the idempotency
 * filter, real controllers, real services and real JPA. That is the point — most of what was fixed
 * in the error-handling work lives in the wiring, not in any one class.
 *
 * <p>Kafka is the exception. A real broker would make these tests slow and flaky, so {@code
 * KafkaTemplate} is mocked and, by default, acknowledges everything. {@link #brokerIsUnreachable()}
 * flips it so the at-least-once rollback path can be exercised.
 */
@SpringBootTest
@AutoConfigureMockMvc
public abstract class ComponentTestBase {

  @Autowired protected MockMvc mockMvc;
  @Autowired protected ObjectMapper objectMapper;

  @MockBean protected KafkaTemplate<String, AuthEvent> kafkaTemplate;

  @Autowired private RoleJpaRepository roleRepository;
  @Autowired private PermissionJpaRepository permissionRepository;
  @Autowired private RolePermissionJpaRepository rolePermissionRepository;
  @Autowired private UserRoleJpaRepository userRoleRepository;
  @Autowired private IdempotencyJpaRepository idempotencyRepository;

  @BeforeEach
  void resetState() {
    // Child tables first: role_permissions and user_roles both reference roles.
    rolePermissionRepository.deleteAll();
    userRoleRepository.deleteAll();
    roleRepository.deleteAll();
    permissionRepository.deleteAll();
    idempotencyRepository.deleteAll();
    brokerAcknowledgesEverything();
  }

  /** The happy path: every send is acked, as a healthy broker would. */
  protected final void brokerAcknowledgesEverything() {
    given(kafkaTemplate.send(any(ProducerRecord.class)))
        .willAnswer(
            invocation -> {
              ProducerRecord<String, AuthEvent> record = invocation.getArgument(0);
              RecordMetadata metadata =
                  new RecordMetadata(new TopicPartition(record.topic(), 0), 0L, 0, 0L, 0, 0);
              return CompletableFuture.completedFuture(new SendResult<>(record, metadata));
            });
  }

  /** Simulates the producer exhausting its retries without an ack. */
  protected final void brokerIsUnreachable() {
    given(kafkaTemplate.send(any(ProducerRecord.class)))
        .willAnswer(
            invocation ->
                CompletableFuture.failedFuture(
                    new IllegalStateException("no in-sync replicas available")));
  }

  // --- Fixtures -----------------------------------------------------------
  // Built through the API rather than the repositories, so a test never sets up state the API
  // itself could not produce.

  protected final UUID createRole(String name) throws Exception {
    return createRole(name, false);
  }

  protected final UUID createRole(String name, boolean system) throws Exception {
    String body =
        mockMvc
            .perform(
                post("/auth/roles")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"name": "%s", "description": "created by a component test", "system": %s}
                        """
                            .formatted(name, system)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return UUID.fromString(JsonPath.read(body, "$.id"));
  }

  protected final UUID createPermission(String resource, String action) throws Exception {
    String body =
        mockMvc
            .perform(
                post("/auth/permissions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"resource": "%s", "action": "%s", "description": "component test"}
                        """
                            .formatted(resource, action)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return UUID.fromString(JsonPath.read(body, "$.id"));
  }

  protected final void assignRole(UUID userId, UUID roleId) throws Exception {
    mockMvc
        .perform(
            post("/auth/users/{userId}/roles", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"roleId": "%s"}
                    """
                        .formatted(roleId)))
        .andExpect(status().isCreated());
  }

  protected final void grantPermissions(UUID roleId, UUID... permissionIds) throws Exception {
    String ids =
        Arrays.stream(permissionIds).map(id -> "\"" + id + "\"").collect(Collectors.joining(","));
    mockMvc
        .perform(
            put("/auth/roles/{id}/permissions", roleId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"permissionIds": [%s]}
                    """
                        .formatted(ids)))
        .andExpect(status().isOk());
  }
}
