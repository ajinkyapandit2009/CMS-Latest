
package com.cmsbackend.controller;

import com.cmsbackend.dtos.UserRequestDto;
import com.cmsbackend.dtos.UserResponseDto;
import com.cmsbackend.services.AdminService;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private AdminService adminService;

    @PostMapping("/case-officer-accounts")
    public ResponseEntity<UserResponseDto> createCaseOfficerAccount(
            @Valid @RequestBody UserRequestDto userRequestDto) {

        UserResponseDto response =
                adminService.createCaseOfficerAccount(userRequestDto);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
