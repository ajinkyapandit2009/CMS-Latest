
package com.cmsbackend.services.impl;

import java.util.Locale;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.web.server.ResponseStatusException;

import com.cmsbackend.dtos.UserRequestDto;
import com.cmsbackend.dtos.CaseOfficerResponseDto;
import com.cmsbackend.dtos.CaseOfficerRequestDto;
import com.cmsbackend.entities.User;
import com.cmsbackend.enums.Role;
import com.cmsbackend.repositories.UserRepository;
import com.cmsbackend.services.AdminService;
import com.cmsbackend.services.CaseOfficerService;

@Service
public class AdminServiceImpl implements AdminService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private CaseOfficerService caseOfficerService;

    @Autowired
    private WorkloadCoordinator workloadCoordinator;

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public CaseOfficerResponseDto createCaseOfficerWithProfile(UserRequestDto request) {
        // Match the profile/complaint lock order before creating either record.
        workloadCoordinator.lock();
        User account = saveOfficerAccount(request);
        CaseOfficerRequestDto profileRequest = new CaseOfficerRequestDto();
        profileRequest.setUserId(account.getUserId());
        // REQUIRED propagation keeps both inserts inside this transaction.
        return caseOfficerService.createCaseOfficer(profileRequest);
    }

    private User saveOfficerAccount(UserRequestDto userRequestDto) {

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
        if (userRepository.existsByEmailIgnoreCase(email)) {
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
        try {
            return userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already registered");
        }
    }
}
