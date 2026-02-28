package com.gymapi.auth.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.topic.auth}")
    private String authTopic;

    @Bean
    public NewTopic authEventsTopic() {
        return TopicBuilder.name(authTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
