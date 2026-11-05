package com.cmsbackend.controller;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cmsbackend.dtos.EscalationResponseDto;
import com.cmsbackend.dtos.EscalationReviewRequestDto;
import com.cmsbackend.enums.EscalationReviewStatus;
import com.cmsbackend.services.impl.ComplaintEscalationService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/escalations")
public class AdminEscalationController {
    private final ComplaintEscalationService service;

    public AdminEscalationController(ComplaintEscalationService service) {
        this.service = service;
    }

    @GetMapping
    public List<EscalationResponseDto> list(@RequestParam(required = false) EscalationReviewStatus status,
            Authentication authentication) {
        return service.allEscalations(status, authentication.getName());
    }

    @PatchMapping("/{escalationId}/review")
    public EscalationResponseDto review(@PathVariable Integer escalationId,
            @Valid @RequestBody EscalationReviewRequestDto request, Authentication authentication) {
        return service.review(escalationId, request.status(), request.remarks(), authentication.getName());
    }
}
