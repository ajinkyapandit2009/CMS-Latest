
package com.cmsbackend.controller;

import com.cmsbackend.dtos.ComplaintRequestDto;
import com.cmsbackend.dtos.ComplaintResponseDto;
import com.cmsbackend.services.ComplaintService;

import jakarta.validation.Valid;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/complaints")
public class ComplaintController {

    @Autowired
    private ComplaintService complaintService;

    @PostMapping
    public ResponseEntity<ComplaintResponseDto> createComplaint(
            @Valid @RequestBody ComplaintRequestDto requestDto,
            Authentication authentication) {

        ComplaintResponseDto response =
                complaintService.createComplaint(
                        requestDto,
                        authentication.getName());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
    
    
    @GetMapping("/my")
    public ResponseEntity<List<ComplaintResponseDto>> getMyComplaints(
            Authentication authentication) {

        List<ComplaintResponseDto> complaints =
                complaintService.getMyComplaints(authentication.getName());

        return ResponseEntity.ok(complaints);
    }
}
