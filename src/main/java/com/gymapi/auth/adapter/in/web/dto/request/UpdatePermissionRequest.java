package com.gymapi.auth.adapter.in.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePermissionRequest {
    @NotBlank(message = "Resource is required")
    @Size(min = 1, max = 50, message = "Resource must be between 1 and 50 characters")
    private String resource;

    @NotBlank(message = "Action is required")
    @Size(min = 1, max = 50, message = "Action must be between 1 and 50 characters")
    private String action;

    private String description;
}
