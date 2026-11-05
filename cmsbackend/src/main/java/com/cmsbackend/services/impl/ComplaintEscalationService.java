package com.cmsbackend.services.impl;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.cmsbackend.dtos.EscalationResponseDto;
import com.cmsbackend.entities.Complaint;
import com.cmsbackend.entities.ComplaintEscalation;
import com.cmsbackend.entities.User;
import com.cmsbackend.enums.ComplaintStatus;
import com.cmsbackend.enums.EscalationReviewStatus;
import com.cmsbackend.enums.Role;
import com.cmsbackend.repositories.ComplaintEscalationRepository;
import com.cmsbackend.repositories.ComplaintRepository;
import com.cmsbackend.repositories.UserRepository;
import com.cmsbackend.services.NotificationService;

@Service
public class ComplaintEscalationService {
    private final ComplaintEscalationRepository escalations;
    private final ComplaintRepository complaints;
    private final UserRepository users;
    private final NotificationService notifications;
    private final WorkloadCoordinator coordinator;
    private final Clock clock;

    public ComplaintEscalationService(ComplaintEscalationRepository escalations,
            ComplaintRepository complaints, UserRepository users,
            NotificationService notifications, WorkloadCoordinator coordinator, Clock clock) {
        this.escalations = escalations;
        this.complaints = complaints;
        this.users = users;
        this.notifications = notifications;
        this.coordinator = coordinator;
        this.clock = clock;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public EscalationResponseDto submit(Integer complaintId, String reason, String email) {
        User citizen = currentUser(email, Role.ROLE_CITIZEN);
        // Follow the existing shared-lock -> complaint-lock order used by complaint updates.
        coordinator.lock();
        Complaint complaint = complaints.findByIdForEscalation(complaintId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Complaint not found"));
        if (!complaint.getCitizen().getUserId().equals(citizen.getUserId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Complaint not found");
        }
        LocalDateTime createdAt = complaint.getCreatedAt();
        boolean terminal = complaint.getStatus() == ComplaintStatus.RESOLVED
                || complaint.getStatus() == ComplaintStatus.REJECTED;
        if (!terminal && (createdAt == null || createdAt.isAfter(LocalDateTime.now(clock).minusHours(48)))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Complaint is not eligible for escalation yet");
        }
        if (escalations.existsByComplaint_ComplaintIdAndReviewStatus(complaintId, EscalationReviewStatus.PENDING)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A pending escalation already exists");
        }
        ComplaintEscalation escalation = new ComplaintEscalation();
        escalation.setComplaint(complaint);
        escalation.setCitizen(complaint.getCitizen());
        escalation.setReason(reason.trim());
        escalation.setReviewStatus(EscalationReviewStatus.PENDING);
        escalation.setPendingMarker(1);
        escalations.saveAndFlush(escalation);
        for (User admin : users.findByUserRole(Role.ROLE_ADMIN)) {
            notifications.createNotification(admin, complaint, "Complaint escalation submitted",
                    "Complaint #" + complaintId + " has a new escalation awaiting review.");
        }
        return toDto(escalation);
    }

    @Transactional(readOnly = true)
    public List<EscalationResponseDto> myEscalations(String email) {
        User citizen = currentUser(email, Role.ROLE_CITIZEN);
        return escalations.findByCitizen_UserIdOrderByCreatedAtDesc(citizen.getUserId())
                .stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public EscalationResponseDto myEscalation(Integer escalationId, String email) {
        User citizen = currentUser(email, Role.ROLE_CITIZEN);
        return toDto(escalations.findByEscalationIdAndCitizen_UserId(escalationId, citizen.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Escalation not found")));
    }

    @Transactional(readOnly = true)
    public List<EscalationResponseDto> allEscalations(EscalationReviewStatus status, String email) {
        currentUser(email, Role.ROLE_ADMIN);
        List<ComplaintEscalation> results = status == null
                ? escalations.findAllByOrderByCreatedAtDesc()
                : escalations.findByReviewStatusOrderByCreatedAtDesc(status);
        return results.stream().map(this::toDto).toList();
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public EscalationResponseDto review(Integer escalationId, EscalationReviewStatus outcome,
            String remarks, String email) {
        User admin = currentUser(email, Role.ROLE_ADMIN);
        if (outcome != EscalationReviewStatus.ACCEPTED && outcome != EscalationReviewStatus.DECLINED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Review outcome must be ACCEPTED or DECLINED");
        }
        ComplaintEscalation escalation = escalations.findByIdForReview(escalationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Escalation not found"));
        if (escalation.getReviewStatus() != EscalationReviewStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Escalation has already been reviewed");
        }
        escalation.setReviewStatus(outcome);
        escalation.setPendingMarker(null);
        escalation.setReviewedAt(LocalDateTime.now(clock));
        escalation.setReviewedBy(admin);
        escalation.setRemarks(remarks.trim());
        escalations.saveAndFlush(escalation);
        notifications.createNotification(escalation.getCitizen().getUser(), escalation.getComplaint(),
                "Complaint escalation reviewed",
                "Your escalation for complaint #" + escalation.getComplaint().getComplaintId()
                        + " was " + outcome.name().toLowerCase() + ".");
        return toDto(escalation);
    }

    private User currentUser(String email, Role role) {
        User user = users.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User account not found"));
        if (user.getUserRole() != role) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }
        return user;
    }

    private EscalationResponseDto toDto(ComplaintEscalation escalation) {
        User citizen = escalation.getCitizen().getUser();
        User reviewer = escalation.getReviewedBy();
        return new EscalationResponseDto(escalation.getEscalationId(),
                escalation.getComplaint().getComplaintId(), escalation.getComplaint().getTitle(),
                citizen.getUserId(), citizen.getFirstName() + " " + citizen.getLastName(),
                escalation.getReason(), escalation.getReviewStatus(), escalation.getCreatedAt(),
                escalation.getReviewedAt(), reviewer == null ? null : reviewer.getUserId(),
                reviewer == null ? null : reviewer.getFirstName() + " " + reviewer.getLastName(),
                escalation.getRemarks());
    }
}
