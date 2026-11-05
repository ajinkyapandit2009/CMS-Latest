
package com.cmsbackend.services.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cmsbackend.dtos.CitizenRegistrationRequestDto;
import com.cmsbackend.dtos.UserResponseDto;
import com.cmsbackend.entities.Citizen;
import com.cmsbackend.entities.User;
import com.cmsbackend.enums.Role;
import com.cmsbackend.repositories.CitizenRepository;
import com.cmsbackend.repositories.UserRepository;
import com.cmsbackend.registration.RegistrationException;
import com.cmsbackend.registration.RegistrationRules;
import com.cmsbackend.services.UserService;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CitizenRepository citizenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public UserResponseDto registerUser(CitizenRegistrationRequestDto request) {
        if (request.getRole() != null || request.getUserRole() != null) {
            throw new RegistrationException(HttpStatus.BAD_REQUEST, "role", "Public registration cannot set an account role");
        }
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new RegistrationException(HttpStatus.BAD_REQUEST, "confirmPassword", "Passwords do not match");
        }
        if (!RegistrationRules.fitsBcryptLimit(request.getPassword())) {
            throw new RegistrationException(HttpStatus.BAD_REQUEST, "password", "Password must be 72 UTF-8 bytes or fewer");
        }
        String address = trimToNull(request.getAddress());
        if (!RegistrationRules.fitsAddressColumn(address)) {
            throw new RegistrationException(HttpStatus.BAD_REQUEST, "address", "Address is too long");
        }
        String email = RegistrationRules.normalizeEmail(request.getEmail());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new RegistrationException(HttpStatus.CONFLICT, "email", "Email is already registered");
        }

        User user = new User();
        user.setFirstName(request.getFirstName().trim());
        user.setLastName(request.getLastName().trim());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setPhone(trimToNull(request.getPhone()));
        user.setAddress(address);
        user.setUserRole(Role.ROLE_CITIZEN);
        try {
            // Flush inside this transaction so a concurrent unique-email conflict is mapped safely.
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException exception) {
            throw new RegistrationException(HttpStatus.CONFLICT, "email", "Email is already registered");
        }
        Citizen citizen = new Citizen();
        citizen.setUser(user);
        citizenRepository.saveAndFlush(citizen);

        UserResponseDto response = new UserResponseDto();
        response.setUserId(user.getUserId());
        response.setFirstName(user.getFirstName());
        response.setLastName(user.getLastName());
        response.setEmail(user.getEmail());
        response.setPhone(user.getPhone());
        response.setAddress(user.getAddress());
        response.setRole(user.getUserRole());
        response.setCreatedAt(user.getCreatedAt());
        return response;
    }

    private static String trimToNull(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }
}
