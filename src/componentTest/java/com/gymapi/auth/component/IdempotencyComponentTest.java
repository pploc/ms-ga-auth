package com.gymapi.auth.component;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gymapi.auth.adapter.in.web.filter.IdempotencyFilter;
import java.util.UUID;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/**
 * The client-facing half of at-least-once: because the service now asks callers to retry, retrying
 * has to be safe.
 */
@DisplayName("Idempotency-Key, end to end")
class IdempotencyComponentTest extends ComponentTestBase {

  private static final String CREATE_ROLE =
      """
      {"name": "FRONT_DESK", "description": "reception"}
      """;

  @Test
  void aRetryWithTheSameKeyReplaysTheOriginalResponseInsteadOfRunningAgain() throws Exception {
    String key = UUID.randomUUID().toString();

    String first =
        mockMvc
            .perform(
                post("/auth/roles")
                    .header(IdempotencyFilter.HEADER_NAME, key)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(CREATE_ROLE))
            .andExpect(status().isCreated())
            .andExpect(header().doesNotExist(IdempotencyFilter.REPLAYED_HEADER))
            .andReturn()
            .getResponse()
            .getContentAsString();

    String replayed =
        mockMvc
            .perform(
                post("/auth/roles")
                    .header(IdempotencyFilter.HEADER_NAME, key)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(CREATE_ROLE))
            .andExpect(status().isCreated())
            .andExpect(header().string(IdempotencyFilter.REPLAYED_HEADER, "true"))
            .andReturn()
            .getResponse()
            .getContentAsString();

    // Byte for byte, including the generated id — the caller cannot tell it was a replay.
    assertThat(replayed).isEqualTo(first);

    // And the work genuinely did not happen twice.
    mockMvc
        .perform(get("/auth/roles"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1));
  }

  @Test
  void withoutAKeyTheSameRetryIsJustADuplicateAndConflicts() throws Exception {
    mockMvc
        .perform(post("/auth/roles").contentType(MediaType.APPLICATION_JSON).content(CREATE_ROLE))
        .andExpect(status().isCreated());

    mockMvc
        .perform(post("/auth/roles").contentType(MediaType.APPLICATION_JSON).content(CREATE_ROLE))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("ROLE_ALREADY_EXISTS"));
  }

  @Test
  void replayingAnAssignmentDoesNotPublishTheEventASecondTime() throws Exception {
    UUID userId = UUID.randomUUID();
    UUID roleId = createRole("MEMBER");
    String key = UUID.randomUUID().toString();
    String body =
        """
        {"roleId": "%s"}
        """
            .formatted(roleId);

    for (int attempt = 0; attempt < 3; attempt++) {
      mockMvc
          .perform(
              post("/auth/users/{userId}/roles", userId)
                  .header(IdempotencyFilter.HEADER_NAME, key)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(body))
          .andExpect(status().isCreated());
    }

    verify(kafkaTemplate, times(1)).send(any(ProducerRecord.class));
  }

  @Test
  void reusingAKeyForADifferentRequestIsRefused() throws Exception {
    String key = UUID.randomUUID().toString();

    mockMvc
        .perform(
            post("/auth/roles")
                .header(IdempotencyFilter.HEADER_NAME, key)
                .contentType(MediaType.APPLICATION_JSON)
                .content(CREATE_ROLE))
        .andExpect(status().isCreated());

    mockMvc
        .perform(
            post("/auth/roles")
                .header(IdempotencyFilter.HEADER_NAME, key)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"name": "SOMETHING_ELSE"}
                    """))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("IDEMPOTENCY_KEY_REUSED"))
        .andExpect(
            jsonPath("$.message").value(org.hamcrest.Matchers.containsString("/auth/roles")));

    // The second, different request was not executed.
    mockMvc.perform(get("/auth/roles")).andExpect(jsonPath("$.length()").value(1));
  }

  @Test
  void theSameKeyOnADifferentEndpointIsAlsoRefused() throws Exception {
    String key = UUID.randomUUID().toString();

    mockMvc
        .perform(
            post("/auth/roles")
                .header(IdempotencyFilter.HEADER_NAME, key)
                .contentType(MediaType.APPLICATION_JSON)
                .content(CREATE_ROLE))
        .andExpect(status().isCreated());

    mockMvc
        .perform(
            post("/auth/permissions")
                .header(IdempotencyFilter.HEADER_NAME, key)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"resource": "booking", "action": "read"}
                    """))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("IDEMPOTENCY_KEY_REUSED"));
  }

  @Test
  void aFailedResponseIsReplayedToo_soTheClientSeesAStableAnswer() throws Exception {
    createRole("TAKEN");
    String key = UUID.randomUUID().toString();
    String duplicate =
        """
        {"name": "TAKEN"}
        """;

    mockMvc
        .perform(
            post("/auth/roles")
                .header(IdempotencyFilter.HEADER_NAME, key)
                .contentType(MediaType.APPLICATION_JSON)
                .content(duplicate))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("ROLE_ALREADY_EXISTS"));

    mockMvc
        .perform(
            post("/auth/roles")
                .header(IdempotencyFilter.HEADER_NAME, key)
                .contentType(MediaType.APPLICATION_JSON)
                .content(duplicate))
        .andExpect(status().isConflict())
        .andExpect(header().string(IdempotencyFilter.REPLAYED_HEADER, "true"))
        .andExpect(jsonPath("$.code").value("ROLE_ALREADY_EXISTS"));
  }

  /**
   * A 5xx is the one response that must not stick: the caller is being told to retry, so the retry
   * has to actually run rather than replay the failure forever.
   */
  @Test
  void aServerErrorIsNotRecorded_soTheRetryRunsForReal() throws Exception {
    UUID userId = UUID.randomUUID();
    UUID roleId = createRole("MEMBER");
    String key = UUID.randomUUID().toString();
    String body =
        """
        {"roleId": "%s"}
        """
            .formatted(roleId);

    brokerIsUnreachable();
    mockMvc
        .perform(
            post("/auth/users/{userId}/roles", userId)
                .header(IdempotencyFilter.HEADER_NAME, key)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.code").value("EVENT_PUBLISH_FAILED"));

    brokerAcknowledgesEverything();
    mockMvc
        .perform(
            post("/auth/users/{userId}/roles", userId)
                .header(IdempotencyFilter.HEADER_NAME, key)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isCreated())
        .andExpect(header().doesNotExist(IdempotencyFilter.REPLAYED_HEADER));

    mockMvc
        .perform(get("/auth/users/{userId}/roles", userId))
        .andExpect(jsonPath("$.length()").value(1));
  }

  @Test
  void deleteIsReplayableToo() throws Exception {
    UUID userId = UUID.randomUUID();
    UUID roleId = createRole("MEMBER");
    assignRole(userId, roleId);
    String key = UUID.randomUUID().toString();

    mockMvc
        .perform(
            delete("/auth/users/{userId}/roles/{roleId}", userId, roleId)
                .header(IdempotencyFilter.HEADER_NAME, key))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("Role removed."));

    // Without the key this second call would be a 404; with it, the original 200 comes back.
    mockMvc
        .perform(
            delete("/auth/users/{userId}/roles/{roleId}", userId, roleId)
                .header(IdempotencyFilter.HEADER_NAME, key))
        .andExpect(status().isOk())
        .andExpect(header().string(IdempotencyFilter.REPLAYED_HEADER, "true"))
        .andExpect(jsonPath("$.message").value("Role removed."));
  }

  @Test
  void aMalformedKeyIsRefusedRatherThanSilentlyIgnored() throws Exception {
    mockMvc
        .perform(
            post("/auth/roles")
                .header(IdempotencyFilter.HEADER_NAME, "has spaces")
                .contentType(MediaType.APPLICATION_JSON)
                .content(CREATE_ROLE))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));

    mockMvc
        .perform(
            post("/auth/roles")
                .header(IdempotencyFilter.HEADER_NAME, "x".repeat(256))
                .contentType(MediaType.APPLICATION_JSON)
                .content(CREATE_ROLE))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));

    mockMvc.perform(get("/auth/roles")).andExpect(jsonPath("$.length()").value(0));
  }

  @Test
  void safeMethodsIgnoreTheHeaderEntirely() throws Exception {
    createRole("MEMBER");
    String key = UUID.randomUUID().toString();

    mockMvc
        .perform(get("/auth/roles").header(IdempotencyFilter.HEADER_NAME, key))
        .andExpect(status().isOk())
        .andExpect(header().doesNotExist(IdempotencyFilter.REPLAYED_HEADER));

    // A GET must not have consumed the key, so it is still free for a mutation.
    mockMvc
        .perform(
            post("/auth/permissions")
                .header(IdempotencyFilter.HEADER_NAME, key)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"resource": "booking", "action": "read"}
                    """))
        .andExpect(status().isCreated());
  }

  @Test
  void aRequestWithoutTheHeaderNeverTouchesTheIdempotencyStore() throws Exception {
    mockMvc
        .perform(post("/auth/roles").contentType(MediaType.APPLICATION_JSON).content(CREATE_ROLE))
        .andExpect(status().isCreated())
        .andExpect(header().doesNotExist(IdempotencyFilter.REPLAYED_HEADER));

    verify(kafkaTemplate, never()).send(any(ProducerRecord.class));
  }
}
