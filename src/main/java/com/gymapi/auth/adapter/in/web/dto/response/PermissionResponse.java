package com.gymapi.auth.adapter.in.web.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PermissionResponse {
    private UUID id;
    private String resource;
    private String action;
    private String description;
    private OffsetDateTime createdAt;
}
