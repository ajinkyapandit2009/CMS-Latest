
package com.cmsbackend.controller;

import com.cmsbackend.dtos.AssignComplaintRequestDto;
import com.cmsbackend.dtos.ComplaintResponseDto;
import com.cmsbackend.services.ComplaintService;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/complaints")
public class ComplaintAssignmentController {

    @Autowired
    private ComplaintService complaintService;

    @PostMapping("/{complaintId}/assign")
    public ResponseEntity<ComplaintResponseDto> assignComplaint(
            @PathVariable Integer complaintId,
            @Valid @RequestBody AssignComplaintRequestDto requestDto,
            Authentication authentication) {

        ComplaintResponseDto response =
                complaintService.assignComplaint(
                        complaintId,
                        requestDto,
                        authentication.getName());

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }
}
