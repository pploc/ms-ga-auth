package com.gymapi.auth.adapter.out.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gymapi.auth.application.port.out.EventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaEventPublisher implements EventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${spring.kafka.topic.auth}")
    private String authTopic;

    @Override
    public void publishRoleAssigned(String userId, String roleId, String assignedBy) {
        publishEvent("auth.role_assigned", Map.of(
                "eventType", "auth.role_assigned",
                "userId", userId,
                "roleId", roleId,
                "assignedBy", assignedBy != null ? assignedBy : "",
                "timestamp", OffsetDateTime.now().toString()
        ));
    }

    @Override
    public void publishRoleRevoked(String userId, String roleId) {
        publishEvent("auth.role_revoked", Map.of(
                "eventType", "auth.role_revoked",
                "userId", userId,
                "roleId", roleId,
                "timestamp", OffsetDateTime.now().toString()
        ));
    }

    @Override
    public void publishPermissionChanged(String roleId, String permissionId, String action) {
        publishEvent("auth.permission_changed", Map.of(
                "eventType", "auth.permission_changed",
                "roleId", roleId,
                "permissionId", permissionId,
                "action", action,
                "timestamp", OffsetDateTime.now().toString()
        ));
    }

    private void publishEvent(String eventType, Map<String, String> event) {
        try {
            String message = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(authTopic, eventType, message);
            log.info("Published event: {}", eventType);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize event: {}", eventType, e);
        }
    }
}
