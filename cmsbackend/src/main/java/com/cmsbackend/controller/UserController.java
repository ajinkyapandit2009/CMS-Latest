package com.cmsbackend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cmsbackend.dtos.UserRequestDto;
import com.cmsbackend.dtos.UserResponseDto;
import com.cmsbackend.services.UserService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/users")
@CrossOrigin(origins = "http://localhost:5173")
public class UserController {

    @Autowired
    private UserService userService;

    // POST - http://localhost:8080/users
    @PostMapping
    public ResponseEntity<UserResponseDto> register(
            @Valid @RequestBody UserRequestDto userRequestDto) {

        UserResponseDto responseDto =
                userService.registerUser(userRequestDto);

        return new ResponseEntity<UserResponseDto>(
                responseDto, HttpStatus.CREATED);
    }
}
