
package com.cmsbackend.services.impl;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.cmsbackend.dtos.CaseOfficerRequestDto;
import com.cmsbackend.dtos.CaseOfficerResponseDto;
import com.cmsbackend.dtos.CaseOfficerWorkloadDto;
import com.cmsbackend.entities.CaseOfficer;
import com.cmsbackend.entities.User;
import com.cmsbackend.enums.Role;
import com.cmsbackend.enums.ComplaintStatus;
import com.cmsbackend.repositories.CaseOfficerRepository;
import com.cmsbackend.repositories.UserRepository;
import com.cmsbackend.services.CaseOfficerService;

@Service
public class CaseOfficerServiceImpl implements CaseOfficerService {
    private static final Pattern EMPLOYEE_NUMBER = Pattern.compile("^CMS-OFF-([0-9]+)$");

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CaseOfficerRepository caseOfficerRepository;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private WorkloadCoordinator workloadCoordinator;

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public CaseOfficerResponseDto createCaseOfficer(
            CaseOfficerRequestDto requestDto) {
        workloadCoordinator.lock();

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

        // 4. Generate employee number automatically
        String employeeNumber = generateEmployeeNumber();

        // 5. Create the entity manually
        // @MapsId derives userId from the associated User
        CaseOfficer caseOfficer = new CaseOfficer();

        caseOfficer.setUser(user);
        caseOfficer.setEmployeeNumber(employeeNumber);

        // 6. Save the officer profile
        CaseOfficer savedCaseOfficer =
                caseOfficerRepository.saveAndFlush(caseOfficer);

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

    @Override
    @Transactional(readOnly = true)
    public List<CaseOfficerResponseDto> getAllCaseOfficers() {
        return caseOfficerRepository.findAllByOrderByEmployeeNumberAsc()
                .stream()
                .map(caseOfficer -> {
                    User user = caseOfficer.getUser();
                    CaseOfficerResponseDto responseDto = new CaseOfficerResponseDto();
                    responseDto.setUserId(caseOfficer.getUserId());
                    responseDto.setEmployeeNumber(caseOfficer.getEmployeeNumber());
                    responseDto.setFirstName(user.getFirstName());
                    responseDto.setLastName(user.getLastName());
                    responseDto.setEmail(user.getEmail());
                    return responseDto;
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CaseOfficerWorkloadDto> getOfficerWorkloads() {
        return caseOfficerRepository.findWorkloadCounts(
                ComplaintStatus.ASSIGNED, ComplaintStatus.IN_PROGRESS)
                .stream()
                .map(count -> {
                    long assigned = count.getAssignedCount();
                    long inProgress = count.getInProgressCount();
                    return new CaseOfficerWorkloadDto(
                            count.getUserId(), count.getFirstName(), count.getLastName(),
                            count.getEmployeeNumber(), assigned, inProgress,
                            assigned + inProgress);
                })
                .toList();
    }

    // Caller holds WorkloadCoordinator's database row lock across this read and insert.
    private String generateEmployeeNumber() {
        long highest = 0;
        for (String value : caseOfficerRepository.findAllEmployeeNumbers()) {
            Matcher match = EMPLOYEE_NUMBER.matcher(value);
            if (match.matches()) {
                try {
                    highest = Math.max(highest, Long.parseLong(match.group(1)));
                } catch (NumberFormatException exception) {
                    throw new IllegalStateException("Existing employee number exceeds the supported range");
                }
            }
        }
        if (highest == Long.MAX_VALUE) throw new IllegalStateException("Employee number range is exhausted");
        return String.format(Locale.ROOT, "CMS-OFF-%03d", highest + 1);
    }
}
