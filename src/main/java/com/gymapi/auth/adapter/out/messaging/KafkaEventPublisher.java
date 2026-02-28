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
        java.util.Map<String, String> data = new java.util.HashMap<>();
        data.put("eventType", "auth.role_assigned");
        data.put("userId", userId);
        data.put("roleId", roleId);
        data.put("assignedBy", assignedBy != null ? assignedBy : "");
        data.put("timestamp", OffsetDateTime.now().toString());
        publishEvent("auth.role_assigned", data);
    }

    @Override
    public void publishRoleRevoked(String userId, String roleId) {
        publishEvent("auth.role_revoked", java.util.Map.of(
                "eventType", "auth.role_revoked",
                "userId", userId,
                "roleId", roleId,
                "timestamp", OffsetDateTime.now().toString()));
    }

    @Override
    public void publishPermissionChanged(String roleId, String roleName, String changeType) {
        publishEvent("auth.permission_changed", java.util.Map.of(
                "eventType", "auth.permission_changed",
                "roleId", roleId,
                "roleName", roleName,
                "changeType", changeType,
                "timestamp", OffsetDateTime.now().toString()));
    }

    private void publishEvent(String eventType, java.util.Map<String, String> event) {
        try {
            String message = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(authTopic, eventType, message);
            log.info("Published event: {}", eventType);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize event: {}", eventType, e);
        }
    }
}
