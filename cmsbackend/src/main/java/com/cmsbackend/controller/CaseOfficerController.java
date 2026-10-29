
package com.cmsbackend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cmsbackend.dtos.CaseOfficerRequestDto;
import com.cmsbackend.dtos.CaseOfficerResponseDto;
import com.cmsbackend.services.CaseOfficerService;

import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/api/admin/case-officers")
public class CaseOfficerController {

    @Autowired
    private CaseOfficerService caseOfficerService;

    @PostMapping
    public ResponseEntity<CaseOfficerResponseDto> createCaseOfficer(
            @Valid @RequestBody
            CaseOfficerRequestDto caseOfficerRequestDto) {

        CaseOfficerResponseDto responseDto =
                caseOfficerService.createCaseOfficer(
                        caseOfficerRequestDto);

        return new ResponseEntity<>(
                responseDto, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<CaseOfficerResponseDto>> getAllCaseOfficers() {
        return ResponseEntity.ok(caseOfficerService.getAllCaseOfficers());
    }
}
