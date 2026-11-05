
package com.cmsbackend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cmsbackend.dtos.CaseOfficerResponseDto;
import com.cmsbackend.dtos.CaseOfficerWorkloadDto;
import com.cmsbackend.services.CaseOfficerService;

import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/admin/case-officers")
public class CaseOfficerController {

    @Autowired
    private CaseOfficerService caseOfficerService;

    /** Existing profiles are retained; new officers must be created atomically. */
    @Deprecated
    @PostMapping
    public void legacyProfileCreationDisabled() {
        throw new ResponseStatusException(HttpStatus.GONE,
                "Use POST /api/admin/case-officers/with-account");
    }

    @GetMapping
    public ResponseEntity<List<CaseOfficerResponseDto>> getAllCaseOfficers() {
        return ResponseEntity.ok(caseOfficerService.getAllCaseOfficers());
    }

    @GetMapping("/workloads")
    public ResponseEntity<List<CaseOfficerWorkloadDto>> getOfficerWorkloads() {
        return ResponseEntity.ok(caseOfficerService.getOfficerWorkloads());
    }
}
