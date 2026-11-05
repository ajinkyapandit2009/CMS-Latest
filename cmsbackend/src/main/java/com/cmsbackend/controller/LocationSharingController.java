package com.cmsbackend.controller;

import java.util.Map;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.cmsbackend.dtos.LocationSharingCreateResponseDto;
import com.cmsbackend.dtos.LocationSharingSessionResponseDto;
import com.cmsbackend.dtos.LocationSubmissionRequestDto;
import com.cmsbackend.services.LocationSharingService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/location-sharing")
public class LocationSharingController {
    private final LocationSharingService locationSharingService;

    public LocationSharingController(LocationSharingService locationSharingService) {
        this.locationSharingService = locationSharingService;
    }

    @PostMapping("/sessions")
    public ResponseEntity<LocationSharingCreateResponseDto> create(Authentication authentication) {
        return ResponseEntity.ok(locationSharingService.create(authentication.getName()));
    }

    @GetMapping("/sessions/{sessionId}")
    public ResponseEntity<LocationSharingSessionResponseDto> get(@PathVariable UUID sessionId, Authentication authentication) {
        return ResponseEntity.ok(locationSharingService.get(sessionId, authentication.getName()));
    }

    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<LocationSharingSessionResponseDto> cancel(@PathVariable UUID sessionId, Authentication authentication) {
        return ResponseEntity.ok(locationSharingService.cancel(sessionId, authentication.getName()));
    }

    @PostMapping("/{token}/location")
    public ResponseEntity<Map<String, String>> submit(@PathVariable String token, @Valid @RequestBody LocationSubmissionRequestDto request) {
        locationSharingService.submit(token, request.getLatitude(), request.getLongitude(), request.getAccuracy());
        return ResponseEntity.ok(Map.of("status", "COMPLETED"));
    }
}
