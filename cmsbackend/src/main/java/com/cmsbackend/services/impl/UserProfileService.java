package com.cmsbackend.services.impl;

import java.nio.charset.StandardCharsets;

import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.cmsbackend.dtos.ProfileUpdateRequestDto;
import com.cmsbackend.dtos.UserResponseDto;
import com.cmsbackend.entities.User;
import com.cmsbackend.enums.Role;
import com.cmsbackend.repositories.CaseOfficerRepository;
import com.cmsbackend.repositories.UserRepository;

@Service
public class UserProfileService {
    private final UserRepository users;
    private final CaseOfficerRepository officers;
    private final ModelMapper mapper;

    public UserProfileService(UserRepository users, CaseOfficerRepository officers, ModelMapper mapper) {
        this.users = users;
        this.officers = officers;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public UserResponseDto getProfile(String email) {
        return toResponse(currentUser(email));
    }

    @Transactional
    public UserResponseDto updateProfile(String email, ProfileUpdateRequestDto request) {
        User user = currentUser(email);
        String address = trimToNull(request.address());
        if (address != null && address.getBytes(StandardCharsets.UTF_8).length > 65535) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Address exceeds the supported storage size");
        }
        user.setPhone(trimToNull(request.phone()));
        user.setAddress(address);
        return toResponse(users.save(user));
    }

    private User currentUser(String email) {
        return users.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User account not found"));
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }

    private UserResponseDto toResponse(User user) {
        UserResponseDto response = mapper.map(user, UserResponseDto.class);
        response.setRole(user.getUserRole());
        response.setEmployeeNumber(user.getUserRole() == Role.ROLE_OFFICER
                ? officers.findById(user.getUserId())
                    .map(officer -> officer.getEmployeeNumber()).orElse(null)
                : null);
        return response;
    }
}
