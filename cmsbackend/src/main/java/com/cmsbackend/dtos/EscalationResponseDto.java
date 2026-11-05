package com.cmsbackend.dtos;

import java.time.LocalDateTime;

import com.cmsbackend.enums.EscalationReviewStatus;

public record EscalationResponseDto(
        Integer escalationId,
        Integer complaintId,
        String complaintTitle,
        Integer citizenId,
        String citizenName,
        String reason,
        EscalationReviewStatus reviewStatus,
        LocalDateTime createdAt,
        LocalDateTime reviewedAt,
        Integer reviewedById,
        String reviewerName,
        String remarks) {
}
