package com.cmsbackend.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cmsbackend.dtos.EscalationCreateRequestDto;
import com.cmsbackend.dtos.EscalationResponseDto;
import com.cmsbackend.services.impl.ComplaintEscalationService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/escalations")
public class ComplaintEscalationController {
    private final ComplaintEscalationService service;

    public ComplaintEscalationController(ComplaintEscalationService service) {
        this.service = service;
    }

    @PostMapping("/complaints/{complaintId}")
    public ResponseEntity<EscalationResponseDto> submit(@PathVariable Integer complaintId,
            @Valid @RequestBody EscalationCreateRequestDto request, Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.submit(complaintId, request.reason(), authentication.getName()));
    }

    @GetMapping("/my")
    public List<EscalationResponseDto> myEscalations(Authentication authentication) {
        return service.myEscalations(authentication.getName());
    }

    @GetMapping("/{escalationId}")
    public EscalationResponseDto myEscalation(@PathVariable Integer escalationId, Authentication authentication) {
        return service.myEscalation(escalationId, authentication.getName());
    }
}
