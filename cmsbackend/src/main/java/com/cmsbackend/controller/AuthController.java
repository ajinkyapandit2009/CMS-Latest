
package com.cmsbackend.controller;

import java.util.Locale;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.cmsbackend.dtos.LoginRequestDto;
import com.cmsbackend.dtos.LoginResponseDto;
import com.cmsbackend.dtos.UserResponseDto;
import com.cmsbackend.entities.User;
import com.cmsbackend.repositories.UserRepository;
import com.cmsbackend.security.jwt.JwtUtils;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private ModelMapper modelMapper;
    
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping("/me")
    public ResponseEntity<UserResponseDto> getCurrentUser(
            Authentication authentication) {

        User user = (User) authentication.getPrincipal();

        UserResponseDto responseDto =
                modelMapper.map(user, UserResponseDto.class);

        return ResponseEntity.ok(responseDto);
    }
    
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(
            @Valid @RequestBody LoginRequestDto loginRequestDto) {

        try {
            // Normalize email
            String email = loginRequestDto.getEmail()
                    .trim()
                    .toLowerCase(Locale.ROOT);

            
            // Authenticate user credentials
            Authentication authentication =
                    authenticationManager.authenticate(
                            new UsernamePasswordAuthenticationToken(
                                    email,
                                    loginRequestDto.getPassword()
                            )
                    );

            // Get the authenticated user
            UserDetails userDetails =
                    (UserDetails) authentication.getPrincipal();

            User user = (User) userDetails;

            // Generate JWT token
            String token =
                    jwtUtils.generateTokenFromUsername(userDetails);

            // Convert User entity to response DTO
            UserResponseDto userResponseDto =
                    modelMapper.map(user, UserResponseDto.class);

            // Build login response
            LoginResponseDto loginResponseDto =
                    new LoginResponseDto(token, userResponseDto);

            return ResponseEntity.ok(loginResponseDto);

        } catch (AuthenticationException exception) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid email or password"
            );
        }
    }
}
