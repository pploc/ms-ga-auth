package com.gymapi.auth.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Role {
    private UUID id;
    private String name;
    private String description;
    private boolean system;
    private List<Permission> permissions;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
