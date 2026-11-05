
package com.cmsbackend.services.impl;

import java.util.List;
import java.util.Comparator;
import java.math.BigDecimal;
import java.util.Objects;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.web.server.ResponseStatusException;

import com.cmsbackend.dtos.AssignComplaintRequestDto;
import com.cmsbackend.dtos.ComplaintRequestDto;
import com.cmsbackend.dtos.ComplaintResponseDto;
import com.cmsbackend.dtos.CaseOfficerWorkloadDto;
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
import com.cmsbackend.services.NotificationService;
import com.cmsbackend.services.ComplaintPhotoService;
import com.cmsbackend.services.CaseOfficerService;

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

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private ComplaintPhotoService complaintPhotoService;

    @Autowired
    private CaseOfficerService caseOfficerService;

    @Autowired
    private WorkloadCoordinator workloadCoordinator;

    // 1. Citizen creates a complaint
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
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

        if (requestDto.getImageUrl() == null || requestDto.getImageUrl().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A CMS-managed complaint photo is required");
        }
        if (requestDto.getAddress() == null || requestDto.getAddress().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A complaint address is required");
        }

        boolean highPriority = HighPriorityCategories.includes(requestDto.getCategory());
        CaseOfficer selectedOfficer = null;
        if (highPriority) {
            workloadCoordinator.lock();
        }

        // The upload row is a durable idempotency marker. Its pessimistic lock serializes
        // concurrent submissions of the same photo; the link survives a lost HTTP response.
        Complaint previous = complaintPhotoService.findComplaintForSubmission(
                requestDto.getImageUrl(), citizen.getUserId(),
                requestDto.getLatitude(), requestDto.getLongitude());
        if (previous != null) {
            if (!sameSubmission(previous, requestDto)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "This complaint photo was used with different complaint details");
            }
            ComplaintResponseDto existing = modelMapper.map(previous, ComplaintResponseDto.class);
            existing.setCitizenId(citizen.getUserId());
            existing.setOfficerId(previous.getOfficer() == null ? null : previous.getOfficer().getUserId());
            return existing;
        }
        if (highPriority) {
            selectedOfficer = selectLeastLoadedOfficer();
        }

        Complaint complaint =
                modelMapper.map(requestDto, Complaint.class);

        complaint.setCitizen(citizen);
        complaint.setOfficer(selectedOfficer);
        complaint.setStatus(selectedOfficer == null ? ComplaintStatus.SUBMITTED : ComplaintStatus.ASSIGNED);

        // Citizens cannot choose the official priority
        complaint.setPriority(highPriority ? ComplaintPriority.HIGH : ComplaintPriority.MEDIUM);

        Complaint savedComplaint =
                complaintRepository.save(complaint);

        complaintPhotoService.consumeForComplaint(
                requestDto.getImageUrl(), savedComplaint,
                requestDto.getLatitude(), requestDto.getLongitude(), email);

        if (selectedOfficer != null) {
            ComplaintHistory history = new ComplaintHistory();
            history.setComplaint(savedComplaint);
            history.setPreviousStatus(ComplaintStatus.SUBMITTED.name());
            history.setNewStatus(ComplaintStatus.ASSIGNED.name());
            history.setRemarks("Automatically assigned to case officer "
                    + selectedOfficer.getEmployeeNumber() + " on HIGH-priority submission");
            complaintHistoryRepository.save(history);

            notificationService.createNotification(user, savedComplaint,
                    "High-Priority Complaint Assigned",
                    "Your complaint #" + savedComplaint.getComplaintId()
                            + " was assigned to a case officer.");
            notificationService.createNotification(selectedOfficer.getUser(), savedComplaint,
                    "High-Priority Complaint Assigned",
                    "Complaint #" + savedComplaint.getComplaintId()
                            + " (" + savedComplaint.getTitle() + ") was assigned to you.");
        } else {
            notificationService.createNotification(user, savedComplaint,
                    "Complaint Submitted",
                    "Your complaint #" + savedComplaint.getComplaintId()
                            + " (" + savedComplaint.getTitle() + ") has been submitted.");
        }
        String adminTitle = highPriority
                ? selectedOfficer == null ? "Unassigned HIGH-Priority Complaint" : "HIGH-Priority Complaint Auto-Assigned"
                : "New Complaint Received";
        String adminMessage = "Complaint #" + savedComplaint.getComplaintId()
                + " (" + savedComplaint.getTitle() + ") "
                + (highPriority ? selectedOfficer == null ? "has no case officer available." : "was assigned automatically."
                        : "requires review.");
        userRepository.findByUserRole(Role.ROLE_ADMIN).forEach(admin ->
                notificationService.createNotification(admin, savedComplaint,
                        adminTitle, adminMessage));

        ComplaintResponseDto responseDto =
                modelMapper.map(
                        savedComplaint, ComplaintResponseDto.class);

        responseDto.setCitizenId(citizen.getUserId());
        responseDto.setOfficerId(selectedOfficer == null ? null : selectedOfficer.getUserId());

        return responseDto;
    }

    private boolean sameSubmission(Complaint previous, ComplaintRequestDto request) {
        return Objects.equals(previous.getTitle(), request.getTitle())
                && Objects.equals(previous.getDescription(), request.getDescription())
                && Objects.equals(previous.getCategory(), request.getCategory())
                && Objects.equals(previous.getAddress(), request.getAddress())
                && Objects.equals(previous.getImageUrl(), request.getImageUrl())
                && sameCoordinate(previous.getLatitude(), request.getLatitude())
                && sameCoordinate(previous.getLongitude(), request.getLongitude());
    }

    private boolean sameCoordinate(BigDecimal left, BigDecimal right) {
        return left == null || right == null ? left == right : left.compareTo(right) == 0;
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

    @Override
    @Transactional(readOnly = true)
    public ComplaintResponseDto getComplaint(Integer complaintId, String email) {
        User requester = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User account not found"));
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Complaint not found"));

        boolean admin = requester.getUserRole() == Role.ROLE_ADMIN;
        boolean owner = requester.getUserRole() == Role.ROLE_CITIZEN
                && complaint.getCitizen().getUserId().equals(requester.getUserId());
        boolean assignedOfficer = requester.getUserRole() == Role.ROLE_OFFICER
                && complaint.getOfficer() != null
                && complaint.getOfficer().getUserId().equals(requester.getUserId());
        if (!admin && !owner && !assignedOfficer) {
            // Use the same response as a missing record to avoid disclosing its existence.
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Complaint not found");
        }

        ComplaintResponseDto response = modelMapper.map(complaint, ComplaintResponseDto.class);
        User citizen = complaint.getCitizen().getUser();
        response.setCitizenId(complaint.getCitizen().getUserId());
        response.setCitizenName(citizen.getFirstName() + " " + citizen.getLastName());
        response.setCitizenEmail(admin || owner ? citizen.getEmail() : null);
        CaseOfficer officer = complaint.getOfficer();
        response.setOfficerId(officer == null ? null : officer.getUserId());
        response.setOfficerName(officer == null ? null : officer.getUser().getFirstName() + " " + officer.getUser().getLastName());
        response.setOfficerEmployeeNumber(officer == null ? null : officer.getEmployeeNumber());
        return response;
    }

    // 3. Admin assigns a complaint to a case officer
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ComplaintResponseDto assignComplaint(
            Integer complaintId,
            AssignComplaintRequestDto requestDto,
            String adminEmail) {

        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Admin account not found"));

        if (admin.getUserRole() != Role.ROLE_ADMIN) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only admins can assign complaints");
        }

        workloadCoordinator.lock();
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Complaint not found"));
        CaseOfficer officer =
                caseOfficerRepository.findById(requestDto.getOfficerId())
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Case officer profile not found"));

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

        CaseOfficer previousOfficer = complaint.getOfficer();
        boolean assignmentChanged = previousOfficer == null
                || !previousOfficer.getUserId().equals(officer.getUserId());
        complaint.setOfficer(officer);
        complaint.setStatus(ComplaintStatus.ASSIGNED);

        Complaint savedComplaint =
                complaintRepository.save(complaint);

        if (assignmentChanged) {
            User citizenUser = savedComplaint.getCitizen().getUser();
            String officerName = officer.getUser().getFirstName() + " "
                    + officer.getUser().getLastName();
            notificationService.createNotification(citizenUser, savedComplaint,
                    "Complaint Assigned",
                    "Complaint #" + savedComplaint.getComplaintId()
                            + " has been assigned to " + officerName + ".");
            notificationService.createNotification(officer.getUser(), savedComplaint,
                    "New Complaint Assigned",
                    "Complaint #" + savedComplaint.getComplaintId()
                            + " (" + savedComplaint.getTitle() + ") has been assigned to you.");
            if (previousOfficer != null) {
                notificationService.createNotification(previousOfficer.getUser(), savedComplaint,
                        "Complaint Assignment Changed",
                        "Complaint #" + savedComplaint.getComplaintId()
                                + " is no longer assigned to you.");
            }
        }

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
    @Transactional(isolation = Isolation.READ_COMMITTED)
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

        workloadCoordinator.lock();

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

        if (previousStatus != newStatus) {
            notificationService.createNotification(savedComplaint.getCitizen().getUser(),
                    savedComplaint, "Complaint Status Updated",
                    "Complaint #" + savedComplaint.getComplaintId() + " changed from "
                            + previousStatus.name() + " to " + newStatus.name() + ".");
        }

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

    // Caller holds the shared workload lock before reading these counts.
    private CaseOfficer selectLeastLoadedOfficer() {
        return caseOfficerService.getOfficerWorkloads().stream()
                .min(Comparator.comparingLong(CaseOfficerWorkloadDto::totalActive)
                        .thenComparing(CaseOfficerWorkloadDto::userId))
                .map(workload -> caseOfficerRepository.findById(workload.userId())
                        .orElseThrow(() -> new IllegalStateException("Selected case officer disappeared")))
                .orElse(null);
    }

    // 7. Admin updates complaint priority and records history
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
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

        if (requestDto.getPriority() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Priority is required");
        }

        // Use the same lock order as manual assignment and status changes.
        workloadCoordinator.lock();
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Complaint not found"));

        ComplaintPriority previousPriority = complaint.getPriority();
        ComplaintPriority newPriority = requestDto.getPriority();
        Complaint savedComplaint = complaint;
        if (previousPriority != newPriority) {
            boolean eligiblePromotion = newPriority == ComplaintPriority.HIGH
                    && complaint.getStatus() == ComplaintStatus.SUBMITTED
                    && complaint.getOfficer() == null;
            CaseOfficer selectedOfficer = eligiblePromotion
                    ? selectLeastLoadedOfficer() : null;

            complaint.setPriority(newPriority);
            if (selectedOfficer != null) {
                complaint.setOfficer(selectedOfficer);
                complaint.setStatus(ComplaintStatus.ASSIGNED);
            }
            savedComplaint = complaintRepository.save(complaint);

            ComplaintHistory priorityHistory = new ComplaintHistory();
            priorityHistory.setComplaint(savedComplaint);
            priorityHistory.setPreviousPriority(previousPriority == null
                    ? null : previousPriority.name());
            priorityHistory.setNewPriority(newPriority.name());
            priorityHistory.setUpdatedBy(admin);
            priorityHistory.setRemarks("Priority changed from "
                    + (previousPriority == null ? "unset" : previousPriority.name())
                    + " to " + newPriority.name());
            complaintHistoryRepository.save(priorityHistory);

            if (selectedOfficer != null) {
                ComplaintHistory assignmentHistory = new ComplaintHistory();
                assignmentHistory.setComplaint(savedComplaint);
                assignmentHistory.setPreviousStatus(ComplaintStatus.SUBMITTED.name());
                assignmentHistory.setNewStatus(ComplaintStatus.ASSIGNED.name());
                assignmentHistory.setRemarks("Automatically assigned to case officer "
                        + selectedOfficer.getEmployeeNumber() + " after HIGH-priority promotion");
                complaintHistoryRepository.save(assignmentHistory);
            }

            User citizenUser = savedComplaint.getCitizen().getUser();
            String message = "Complaint #" + savedComplaint.getComplaintId()
                    + " priority changed from "
                    + (previousPriority == null ? "unset" : previousPriority.name())
                    + " to " + newPriority.name() + ".";
            notificationService.createNotification(citizenUser, savedComplaint,
                    selectedOfficer == null ? "Complaint Priority Updated" : "High-Priority Complaint Assigned",
                    selectedOfficer == null ? message : message + " A case officer was assigned automatically.");
            if (savedComplaint.getOfficer() != null) {
                notificationService.createNotification(savedComplaint.getOfficer().getUser(),
                        savedComplaint,
                        selectedOfficer == null ? "Complaint Priority Changed" : "High-Priority Complaint Assigned",
                        selectedOfficer == null ? message
                                : "Complaint #" + savedComplaint.getComplaintId() + " was assigned to you automatically.");
            }
            if (eligiblePromotion) {
                String adminTitle = selectedOfficer == null
                        ? "Unassigned HIGH-Priority Complaint" : "HIGH-Priority Complaint Auto-Assigned";
                String adminMessage = "Complaint #" + savedComplaint.getComplaintId()
                        + (selectedOfficer == null
                                ? " was promoted to HIGH, but no case officer is available."
                                : " was promoted to HIGH and assigned automatically.");
                for (User recipient : userRepository.findByUserRole(Role.ROLE_ADMIN)) {
                    notificationService.createNotification(recipient, savedComplaint,
                            adminTitle, adminMessage);
                }
            }
        }

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
