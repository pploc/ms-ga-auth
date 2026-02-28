package com.gymapi.auth.adapter.out.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KafkaEventPublisherTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private KafkaEventPublisher kafkaEventPublisher;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(kafkaEventPublisher, "authTopic", "auth-test-topic");
    }

    @Test
    void publishRoleAssigned_Success() throws JsonProcessingException {
        String userId = UUID.randomUUID().toString();
        String roleId = UUID.randomUUID().toString();

        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        kafkaEventPublisher.publishRoleAssigned(userId, roleId, "admin");

        verify(kafkaTemplate).send(eq("auth-test-topic"), eq("auth.role_assigned"), anyString());
    }

    @Test
    void publishRoleRevoked_Success() throws JsonProcessingException {
        String userId = UUID.randomUUID().toString();
        String roleId = UUID.randomUUID().toString();

        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        kafkaEventPublisher.publishRoleRevoked(userId, roleId);

        verify(kafkaTemplate).send(eq("auth-test-topic"), eq("auth.role_revoked"), anyString());
    }

    @Test
    void publishPermissionChanged_Success() throws JsonProcessingException {
        String roleId = UUID.randomUUID().toString();

        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        kafkaEventPublisher.publishPermissionChanged(roleId, "ADMIN", "updated");

        verify(kafkaTemplate).send(eq("auth-test-topic"), eq("auth.permission_changed"), anyString());
    }

    @Test
    void publishEvent_JsonProcessingException() throws JsonProcessingException {
        when(objectMapper.writeValueAsString(any())).thenThrow(new JsonProcessingException("error") {
        });

        kafkaEventPublisher.publishRoleRevoked(UUID.randomUUID().toString(), UUID.randomUUID().toString());

        // Should catch and log error, not throw
    }
}
