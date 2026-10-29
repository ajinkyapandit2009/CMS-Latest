
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
import com.cmsbackend.entities.Citizen;
import com.cmsbackend.entities.User;
import com.cmsbackend.repositories.CitizenRepository;
import com.cmsbackend.repositories.UserRepository;
import com.cmsbackend.services.UserService;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CitizenRepository citizenRepository;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    @Transactional // databse rollback possible
    public UserResponseDto registerUser(UserRequestDto userRequestDto) {

        // Check whether the passwords match
        if (!userRequestDto.getPassword()
                .equals(userRequestDto.getConfirmPassword())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Passwords do not match");
        }

        // Normalize the email address
        String email = userRequestDto.getEmail()
                .trim()
                .toLowerCase(Locale.ROOT);

        // Check whether the email is already registered
        if (userRepository.findByEmail(email).isPresent()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Email is already registered");
        }

        // Convert DTO to User entity
        User user = modelMapper.map(userRequestDto, User.class);

        user.setEmail(email);

        // Assign the role on the backend
        user.setUserRole("CITIZEN");

        // Encode  password
        String encodedPassword =
                passwordEncoder.encode(user.getPassword());

        // Set encoded password  for user
        user.setPassword(encodedPassword);

        // Save the User entity to database
        User savedUser = userRepository.save(user);

        // Create the corresponding Citizen profile
        Citizen citizen = new Citizen();
        citizen.setUser(savedUser);

        citizenRepository.save(citizen);

        // Convert saved User entity to response DTO
        UserResponseDto savedDto =
                modelMapper.map(savedUser, UserResponseDto.class);

        return savedDto;
    }
}
