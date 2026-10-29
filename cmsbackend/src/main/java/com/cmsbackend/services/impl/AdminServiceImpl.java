
package com.cmsbackend.services.impl;

import java.util.Locale;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.cmsbackend.dtos.UserRequestDto;
import com.cmsbackend.dtos.UserResponseDto;
import com.cmsbackend.entities.User;
import com.cmsbackend.enums.Role;
import com.cmsbackend.repositories.UserRepository;
import com.cmsbackend.services.AdminService;

@Service
public class AdminServiceImpl implements AdminService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public UserResponseDto createCaseOfficerAccount(
            UserRequestDto userRequestDto) {

        // Check whether passwords match
        if (!userRequestDto.getPassword()
                .equals(userRequestDto.getConfirmPassword())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Passwords do not match");
        }

        // Normalize email
        String email = userRequestDto.getEmail()
                .trim()
                .toLowerCase(Locale.ROOT);

        // Check for duplicate email
        if (userRepository.findByEmail(email).isPresent()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Email is already registered");
        }

        // Convert request DTO to User entity
        User user = modelMapper.map(userRequestDto, User.class);

        user.setEmail(email);
        user.setUserRole(Role.ROLE_OFFICER);
        user.setPassword(
                passwordEncoder.encode(user.getPassword()));

        // Save officer account
        User savedUser = userRepository.save(user);

        // Convert saved entity to response DTO
        return modelMapper.map(savedUser, UserResponseDto.class);
    }
}
