package com.cmsbackend.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EscalationCreateRequestDto(
        @NotBlank(message = "Escalation reason is required")
        @Size(max = 4000, message = "Escalation reason must be 4000 characters or fewer")
        String reason) {
}
