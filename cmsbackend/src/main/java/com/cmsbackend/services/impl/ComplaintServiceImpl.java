
package com.cmsbackend.services.impl;

import java.util.List;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.cmsbackend.dtos.AssignComplaintRequestDto;
import com.cmsbackend.dtos.ComplaintRequestDto;
import com.cmsbackend.dtos.ComplaintResponseDto;
import com.cmsbackend.dtos.UpdateComplaintPriorityRequestDto;
import com.cmsbackend.dtos.UpdateComplaintStatusRequestDto;
import com.cmsbackend.entities.CaseOfficer;
import com.cmsbackend.entities.Citizen;
import com.cmsbackend.entities.Complaint;
import com.cmsbackend.entities.ComplaintHistory;
import com.cmsbackend.entities.User;
import com.cmsbackend.enums.ComplaintPriority;
import com.cmsbackend.enums.ComplaintStatus;
import com.cmsbackend.enums.Role;
import com.cmsbackend.repositories.CaseOfficerRepository;
import com.cmsbackend.repositories.CitizenRepository;
import com.cmsbackend.repositories.ComplaintHistoryRepository;
import com.cmsbackend.repositories.ComplaintRepository;
import com.cmsbackend.repositories.UserRepository;
import com.cmsbackend.services.ComplaintService;

@Service
public class ComplaintServiceImpl implements ComplaintService {

    @Autowired
    private ComplaintRepository complaintRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CitizenRepository citizenRepository;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private CaseOfficerRepository caseOfficerRepository;

    @Autowired
    private ComplaintHistoryRepository complaintHistoryRepository;

    // 1. Citizen creates a complaint
    @Override
    @Transactional
    public ComplaintResponseDto createComplaint(
            ComplaintRequestDto requestDto, String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User account not found"));

        if (user.getUserRole() != Role.ROLE_CITIZEN) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only citizens can submit complaints");
        }

        Citizen citizen = citizenRepository.findById(user.getUserId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Citizen profile not found"));

        Complaint complaint =
                modelMapper.map(requestDto, Complaint.class);

        complaint.setCitizen(citizen);
        complaint.setStatus(ComplaintStatus.SUBMITTED);

        // Citizens cannot choose the official priority
        complaint.setPriority(ComplaintPriority.MEDIUM);

        Complaint savedComplaint =
                complaintRepository.save(complaint);

        ComplaintResponseDto responseDto =
                modelMapper.map(
                        savedComplaint, ComplaintResponseDto.class);

        responseDto.setCitizenId(citizen.getUserId());
        responseDto.setOfficerId(null);

        return responseDto;
    }

    // 2. Citizen views their own complaints
    @Override
    @Transactional(readOnly = true)
    public List<ComplaintResponseDto> getMyComplaints(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User account not found"));

        if (user.getUserRole() != Role.ROLE_CITIZEN) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only citizens can view their complaints");
        }

        Citizen citizen = citizenRepository.findById(user.getUserId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Citizen profile not found"));

        List<Complaint> complaints =
                complaintRepository
                        .findByCitizen_UserIdOrderByCreatedAtDesc(
                                citizen.getUserId());

        return complaints.stream().map(complaint -> {
            ComplaintResponseDto dto =
                    modelMapper.map(
                            complaint, ComplaintResponseDto.class);

            dto.setCitizenId(citizen.getUserId());
            dto.setOfficerId(
                    complaint.getOfficer() == null
                            ? null
                            : complaint.getOfficer().getUserId());

            return dto;
        }).toList();
    }

    // 3. Admin assigns a complaint to a case officer
    @Override
    @Transactional
    public ComplaintResponseDto assignComplaint(
            Integer complaintId,
            AssignComplaintRequestDto requestDto,
            String adminEmail) {

        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Complaint not found"));

        CaseOfficer officer =
                caseOfficerRepository.findById(requestDto.getOfficerId())
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Case officer profile not found"));

        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Admin account not found"));

        if (admin.getUserRole() != Role.ROLE_ADMIN) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only admins can assign complaints");
        }

        if (complaint.getStatus() == ComplaintStatus.RESOLVED
                || complaint.getStatus() == ComplaintStatus.REJECTED
                || complaint.getStatus() == ComplaintStatus.IN_PROGRESS) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This complaint cannot be assigned in its current status");
        }

        String previousStatus =
                complaint.getStatus() == null
                        ? null
                        : complaint.getStatus().name();

        complaint.setOfficer(officer);
        complaint.setStatus(ComplaintStatus.ASSIGNED);

        Complaint savedComplaint =
                complaintRepository.save(complaint);

        // Record assignment in complaint history
        ComplaintHistory history = new ComplaintHistory();

        history.setComplaint(savedComplaint);
        history.setPreviousStatus(previousStatus);
        history.setNewStatus(ComplaintStatus.ASSIGNED.name());
        history.setUpdatedBy(admin);

        String remarks = requestDto.getRemarks();

        if (remarks == null || remarks.isBlank()) {
            remarks = "Assigned to case officer "
                    + officer.getEmployeeNumber();
        }

        history.setRemarks(remarks.trim());
        complaintHistoryRepository.save(history);

        ComplaintResponseDto responseDto =
                modelMapper.map(
                        savedComplaint, ComplaintResponseDto.class);

        responseDto.setCitizenId(
                savedComplaint.getCitizen().getUserId());
        responseDto.setOfficerId(
                savedComplaint.getOfficer().getUserId());

        return responseDto;
    }

    // 4. Case officer views assigned complaints
    @Override
    @Transactional(readOnly = true)
    public List<ComplaintResponseDto> getAssignedComplaints(
            String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User account not found"));

        if (user.getUserRole() != Role.ROLE_OFFICER) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only case officers can view assigned complaints");
        }

        CaseOfficer officer =
                caseOfficerRepository.findById(user.getUserId())
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Case officer profile not found"));

        List<Complaint> complaints =
                complaintRepository
                        .findByOfficer_UserIdOrderByCreatedAtDesc(
                                officer.getUserId());

        return complaints.stream().map(complaint -> {
            ComplaintResponseDto dto =
                    modelMapper.map(
                            complaint, ComplaintResponseDto.class);

            dto.setCitizenId(complaint.getCitizen().getUserId());
            dto.setOfficerId(officer.getUserId());

            return dto;
        }).toList();
    }

    // 5. Case officer updates complaint status
    @Override
    @Transactional
    public ComplaintResponseDto updateComplaintStatus(
            Integer complaintId,
            UpdateComplaintStatusRequestDto requestDto,
            String officerEmail) {

        User officerUser = userRepository.findByEmail(officerEmail)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User account not found"));

        if (officerUser.getUserRole() != Role.ROLE_OFFICER) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only case officers can update complaint status");
        }

        CaseOfficer officer =
                caseOfficerRepository.findById(officerUser.getUserId())
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Case officer profile not found"));

        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Complaint not found"));

        if (complaint.getOfficer() == null
                || !officer.getUserId().equals(
                        complaint.getOfficer().getUserId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "This complaint is not assigned to you");
        }

        ComplaintStatus previousStatus = complaint.getStatus();
        ComplaintStatus newStatus = requestDto.getStatus();

        boolean allowed =
                (previousStatus == ComplaintStatus.ASSIGNED
                        && (newStatus == ComplaintStatus.IN_PROGRESS
                        || newStatus == ComplaintStatus.REJECTED))
                || (previousStatus == ComplaintStatus.IN_PROGRESS
                        && (newStatus == ComplaintStatus.RESOLVED
                        || newStatus == ComplaintStatus.REJECTED));

        if (!allowed) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This status transition is not allowed");
        }

        complaint.setStatus(newStatus);

        Complaint savedComplaint =
                complaintRepository.save(complaint);

        // Record status change in complaint history
        ComplaintHistory history = new ComplaintHistory();

        history.setComplaint(savedComplaint);
        history.setPreviousStatus(previousStatus.name());
        history.setNewStatus(newStatus.name());
        history.setUpdatedBy(officerUser);

        String remarks = requestDto.getRemarks();

        if (remarks == null || remarks.isBlank()) {
            remarks = "Status changed from "
                    + previousStatus.name()
                    + " to " + newStatus.name();
        }

        history.setRemarks(remarks.trim());
        complaintHistoryRepository.save(history);

        ComplaintResponseDto responseDto =
                modelMapper.map(
                        savedComplaint, ComplaintResponseDto.class);

        responseDto.setCitizenId(
                savedComplaint.getCitizen().getUserId());
        responseDto.setOfficerId(officer.getUserId());

        return responseDto;
    }

    // 6. Admin views and filters all complaints
    @Override
    @Transactional(readOnly = true)
    public List<ComplaintResponseDto> getAllComplaints(
            String adminEmail,
            ComplaintStatus status,
            ComplaintPriority priority) {

        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Admin account not found"));

        if (admin.getUserRole() != Role.ROLE_ADMIN) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only admins can view all complaints");
        }

        List<Complaint> complaints =
                complaintRepository.findWithOptionalFilters(
                        status, priority);

        return complaints.stream().map(complaint -> {
            ComplaintResponseDto dto =
                    modelMapper.map(
                            complaint, ComplaintResponseDto.class);

            Citizen citizen = complaint.getCitizen();
            User citizenUser = citizen.getUser();

            dto.setCitizenId(citizen.getUserId());
            dto.setCitizenName(
                    citizenUser.getFirstName()
                            + " " + citizenUser.getLastName());
            dto.setCitizenEmail(citizenUser.getEmail());

            CaseOfficer officer = complaint.getOfficer();

            if (officer != null) {
                User officerUser = officer.getUser();

                dto.setOfficerId(officer.getUserId());
                dto.setOfficerName(
                        officerUser.getFirstName()
                                + " " + officerUser.getLastName());
                dto.setOfficerEmployeeNumber(
                        officer.getEmployeeNumber());
            } else {
                dto.setOfficerId(null);
                dto.setOfficerName(null);
                dto.setOfficerEmployeeNumber(null);
            }

            return dto;
        }).toList();
    }

    // 7. Admin updates complaint priority and records history
    @Override
    @Transactional
    public ComplaintResponseDto updateComplaintPriority(
            Integer complaintId,
            UpdateComplaintPriorityRequestDto requestDto,
            String adminEmail) {

        // Verify the logged-in admin
        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Admin account not found"));

        if (admin.getUserRole() != Role.ROLE_ADMIN) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only admins can update complaint priority");
        }

        // Find the complaint
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Complaint not found"));

        if (requestDto.getPriority() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Priority is required");
        }

        // Capture the previous and requested priorities
        ComplaintPriority previousPriority = complaint.getPriority();
        ComplaintPriority newPriority = requestDto.getPriority();

        // Update and save the complaint
        complaint.setPriority(newPriority);
        Complaint savedComplaint =
                complaintRepository.save(complaint);

        // Record the priority change in complaint history
        ComplaintHistory history = new ComplaintHistory();

        history.setComplaint(savedComplaint);

        history.setPreviousPriority(
                previousPriority == null
                        ? null
                        : previousPriority.name());

        history.setNewPriority(newPriority.name());
        history.setUpdatedBy(admin);

        String oldPriorityText =
                previousPriority == null
                        ? "unset"
                        : previousPriority.name();

        history.setRemarks(
                "Priority changed from "
                        + oldPriorityText
                        + " to " + newPriority.name());

        complaintHistoryRepository.save(history);

        // Return the updated complaint
        ComplaintResponseDto responseDto =
                modelMapper.map(
                        savedComplaint, ComplaintResponseDto.class);

        responseDto.setCitizenId(
                savedComplaint.getCitizen().getUserId());

        responseDto.setOfficerId(
                savedComplaint.getOfficer() == null
                        ? null
                        : savedComplaint.getOfficer().getUserId());

        return responseDto;
    }
}
