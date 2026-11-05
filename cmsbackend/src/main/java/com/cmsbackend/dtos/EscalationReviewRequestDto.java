package com.cmsbackend.dtos;

import com.cmsbackend.enums.EscalationReviewStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record EscalationReviewRequestDto(
        @NotNull(message = "Review outcome is required")
        EscalationReviewStatus status,
        @NotBlank(message = "Review remarks are required")
        @Size(max = 4000, message = "Review remarks must be 4000 characters or fewer")
        String remarks) {
}
