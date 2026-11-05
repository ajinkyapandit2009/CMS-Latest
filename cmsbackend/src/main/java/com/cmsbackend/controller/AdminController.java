
package com.cmsbackend.controller;

import com.cmsbackend.dtos.UserRequestDto;
import com.cmsbackend.dtos.CaseOfficerResponseDto;
import com.cmsbackend.services.AdminService;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private AdminService adminService;

    /** Kept as a tombstone so old clients receive a clear migration response. */
    @Deprecated
    @PostMapping("/case-officer-accounts")
    public void legacyAccountCreationDisabled() {
        throw new ResponseStatusException(HttpStatus.GONE,
                "Use POST /api/admin/case-officers/with-account");
    }

    @PostMapping("/case-officers/with-account")
    public ResponseEntity<CaseOfficerResponseDto> createCaseOfficerWithProfile(
            @Valid @RequestBody UserRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(adminService.createCaseOfficerWithProfile(request));
    }
}
