package com.cmsbackend.dtos;

public record CaseOfficerWorkloadDto(
        Integer userId,
        String firstName,
        String lastName,
        String employeeNumber,
        long assignedCount,
        long inProgressCount,
        long totalActive) {
}
