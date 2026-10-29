package com.cmsbackend.controller;

import java.util.List;

import com.cmsbackend.dtos.ComplaintResponseDto;
import com.cmsbackend.services.ComplaintService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/officer/complaints")
public class OfficerComplaintController {

    @Autowired
    private ComplaintService complaintService;

    @GetMapping("/assigned")
    public ResponseEntity<List<ComplaintResponseDto>> getAssignedComplaints(
            Authentication authentication) {

        List<ComplaintResponseDto> complaints =
                complaintService.getAssignedComplaints(
                        authentication.getName());

        return ResponseEntity.ok(complaints);
    }
}