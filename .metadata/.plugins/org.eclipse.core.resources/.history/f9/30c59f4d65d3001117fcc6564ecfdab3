package com.cmsbackend.services.impl;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.cmsbackend.dtos.CaseOfficerRequestDto;
import com.cmsbackend.dtos.CaseOfficerResponseDto;
import com.cmsbackend.entities.CaseOfficer;
import com.cmsbackend.entities.User;
import com.cmsbackend.enums.Role;
import com.cmsbackend.repositories.CaseOfficerRepository;
import com.cmsbackend.repositories.UserRepository;
import com.cmsbackend.services.CaseOfficerService;

@Service
public class CaseOfficerServiceImpl implements CaseOfficerService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CaseOfficerRepository caseOfficerRepository;

    @Autowired
    private ModelMapper modelMapper;

    @Override
    @Transactional
    public CaseOfficerResponseDto createCaseOfficer(
            CaseOfficerRequestDto requestDto) {

        // 1. Find the existing user account
        User user = userRepository.findById(requestDto.getUserId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User account not found"));

        // 2. Make sure the user has the officer role
        if (user.getUserRole() != Role.ROLE_OFFICER) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "The selected user does not have the officer role");
        }

        // 3. Check whether this user already has an officer profile
        if (caseOfficerRepository.existsById(requestDto.getUserId())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A case officer profile already exists for this user");
        }

        // 4. Check employee number uniqueness
        String employeeNumber =
                requestDto.getEmployeeNumber().trim();

        if (caseOfficerRepository.existsByEmployeeNumber(employeeNumber)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Employee number already exists");
        }

        // 5. Create the entity manually
        // @MapsId derives userId from the associated User
        CaseOfficer caseOfficer = new CaseOfficer();

        caseOfficer.setUser(user);
        caseOfficer.setDepartment(requestDto.getDepartment());
        caseOfficer.setDesignation(requestDto.getDesignation());
        caseOfficer.setEmployeeNumber(employeeNumber);

        // 6. Save the officer profile
        CaseOfficer savedCaseOfficer =
                caseOfficerRepository.save(caseOfficer);

        // 7. Map the saved entity to the response DTO
        CaseOfficerResponseDto responseDto =
                modelMapper.map(
                        savedCaseOfficer,
                        CaseOfficerResponseDto.class);

        responseDto.setUserId(savedCaseOfficer.getUserId());
        responseDto.setFirstName(user.getFirstName());
        responseDto.setLastName(user.getLastName());
        responseDto.setEmail(user.getEmail());

        return responseDto;
    }
}