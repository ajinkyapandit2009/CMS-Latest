package com.cmsbackend.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import com.cmsbackend.dtos.*;
import com.cmsbackend.entities.*;
import com.cmsbackend.enums.*;
import com.cmsbackend.repositories.*;
import com.cmsbackend.services.impl.ComplaintServiceImpl;
import com.cmsbackend.services.impl.HighPriorityCategories;
import com.cmsbackend.services.impl.WorkloadCoordinator;

class ComplaintWorkflowRegressionTest {
    private final ComplaintRepository complaints = mock(ComplaintRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final CitizenRepository citizens = mock(CitizenRepository.class);
    private final CaseOfficerRepository officers = mock(CaseOfficerRepository.class);
    private final ComplaintHistoryRepository history = mock(ComplaintHistoryRepository.class);
    private final NotificationService notifications = mock(NotificationService.class);
    private final ComplaintPhotoService photos = mock(ComplaintPhotoService.class);
    private final CaseOfficerService caseOfficerService = mock(CaseOfficerService.class);
    private final WorkloadCoordinator coordinator = mock(WorkloadCoordinator.class);
    private final ComplaintServiceImpl service = new ComplaintServiceImpl();
    private final User citizen = user(1, Role.ROLE_CITIZEN);
    private final User admin = user(2, Role.ROLE_ADMIN);
    private final User officerUser = user(3, Role.ROLE_OFFICER);
    private final User otherOfficerUser = user(4, Role.ROLE_OFFICER);
    private final Citizen citizenProfile = new Citizen();
    private final CaseOfficer officer = caseOfficer(officerUser);
    private final CaseOfficer otherOfficer = caseOfficer(otherOfficerUser);
    private final Complaint complaint = new Complaint();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "complaintRepository", complaints);
        ReflectionTestUtils.setField(service, "userRepository", users);
        ReflectionTestUtils.setField(service, "citizenRepository", citizens);
        ReflectionTestUtils.setField(service, "caseOfficerRepository", officers);
        ReflectionTestUtils.setField(service, "complaintHistoryRepository", history);
        ReflectionTestUtils.setField(service, "notificationService", notifications);
        ReflectionTestUtils.setField(service, "complaintPhotoService", photos);
        ReflectionTestUtils.setField(service, "caseOfficerService", caseOfficerService);
        ReflectionTestUtils.setField(service, "workloadCoordinator", coordinator);
        ReflectionTestUtils.setField(service, "modelMapper", new ModelMapper());
        citizenProfile.setUserId(1);
        citizenProfile.setUser(citizen);
        complaint.setComplaintId(17);
        complaint.setCitizen(citizenProfile);
        complaint.setOfficer(officer);
        complaint.setTitle("Safe fixture issue");
        complaint.setStatus(ComplaintStatus.ASSIGNED);
        complaint.setPriority(ComplaintPriority.MEDIUM);
        when(complaints.findById(17)).thenReturn(Optional.of(complaint));
        when(complaints.save(any(Complaint.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(officers.findById(3)).thenReturn(Optional.of(officer));
    }

    private static User user(int id, Role role) {
        User user = new User();
        user.setUserId(id);
        user.setUserRole(role);
        user.setFirstName("Fixture");
        user.setLastName(String.valueOf(id));
        user.setEmail("fixture" + id + "@example.test");
        return user;
    }

    private static CaseOfficer caseOfficer(User user) {
        CaseOfficer officer = new CaseOfficer();
        officer.setUserId(user.getUserId());
        officer.setUser(user);
        officer.setEmployeeNumber("CMS-OFF-" + user.getUserId());
        return officer;
    }

    private void as(User user) {
        when(users.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
    }

    private void forbidden(Runnable action) {
        assertEquals(HttpStatus.FORBIDDEN, assertThrows(ResponseStatusException.class, action::run).getStatusCode());
    }

    @Test
    void citizenCreationSetsOfficialDefaultsAndConsumesManagedPhotoOnce() {
        as(citizen);
        when(citizens.findById(1)).thenReturn(Optional.of(citizenProfile));
        when(users.findByUserRole(Role.ROLE_ADMIN)).thenReturn(List.of(admin));
        ComplaintRequestDto request = new ComplaintRequestDto();
        request.setTitle("Safe fixture issue");
        request.setDescription("Fixture description");
        request.setCategory("Roads");
        request.setAddress("Verified fixture address");
        request.setLatitude(new BigDecimal("18.12345678"));
        request.setLongitude(new BigDecimal("73.12345678"));
        request.setImageUrl("/api/complaint-photos/12345678-1234-1234-1234-123456789abc/content");

        var response = service.createComplaint(request, citizen.getEmail());

        ArgumentCaptor<Complaint> saved = ArgumentCaptor.forClass(Complaint.class);
        verify(complaints).save(saved.capture());
        assertEquals(ComplaintStatus.SUBMITTED, saved.getValue().getStatus());
        assertEquals(ComplaintPriority.MEDIUM, saved.getValue().getPriority());
        assertEquals(1, response.getCitizenId());
        verify(photos).consumeForComplaint(eq(request.getImageUrl()), any(Complaint.class), eq(request.getLatitude()), eq(request.getLongitude()), eq(citizen.getEmail()));
        verify(notifications).createNotification(eq(citizen), any(Complaint.class), eq("Complaint Submitted"), anyString());
        verify(notifications).createNotification(eq(admin), any(Complaint.class), eq("New Complaint Received"), anyString());
    }

    @Test
    void nonCitizenCannotCreate() {
        as(admin);
        forbidden(() -> service.createComplaint(new ComplaintRequestDto(), admin.getEmail()));
        verify(complaints, never()).save(any());
    }

    @Test
    void urgentCategoriesAreClassifiedAndLeastLoadedOfficerWinsUserIdTie() {
        assertTrue(HighPriorityCategories.includes("Gas Leakage"));
        assertTrue(HighPriorityCategories.includes("Water Pipeline Burst"));
        assertTrue(HighPriorityCategories.includes("Power Cut"));
        assertTrue(HighPriorityCategories.includes(" gas leakage "));
        assertFalse(HighPriorityCategories.includes("Potholes and damaged roads"));

        as(citizen);
        when(citizens.findById(1)).thenReturn(Optional.of(citizenProfile));
        when(users.findByUserRole(Role.ROLE_ADMIN)).thenReturn(List.of(admin));
        when(caseOfficerService.getOfficerWorkloads()).thenReturn(List.of(
                new CaseOfficerWorkloadDto(4, "Fixture", "4", "CMS-OFF-4", 0, 0, 0),
                new CaseOfficerWorkloadDto(3, "Fixture", "3", "CMS-OFF-3", 0, 0, 0)));
        ComplaintRequestDto request = new ComplaintRequestDto();
        request.setTitle("Urgent fixture");
        request.setDescription("Fixture description");
        request.setCategory("Gas Leakage");
        request.setAddress("Verified fixture address");
        request.setLatitude(new BigDecimal("18.12345678"));
        request.setLongitude(new BigDecimal("73.12345678"));
        request.setImageUrl("/api/complaint-photos/12345678-1234-1234-1234-123456789abc/content");

        var response = service.createComplaint(request, citizen.getEmail());

        assertEquals(ComplaintPriority.HIGH, response.getPriority());
        assertEquals(ComplaintStatus.ASSIGNED, response.getStatus());
        assertEquals(3, response.getOfficerId());
        verify(coordinator).lock();
        verify(caseOfficerService).getOfficerWorkloads();
        ArgumentCaptor<ComplaintHistory> historyCaptor = ArgumentCaptor.forClass(ComplaintHistory.class);
        verify(history).save(historyCaptor.capture());
        assertEquals("SUBMITTED", historyCaptor.getValue().getPreviousStatus());
        assertEquals("ASSIGNED", historyCaptor.getValue().getNewStatus());
        assertNull(historyCaptor.getValue().getUpdatedBy());
        verify(notifications).createNotification(eq(citizen), any(Complaint.class), anyString(), anyString());
        verify(notifications).createNotification(eq(officerUser), any(Complaint.class), anyString(), anyString());
        verify(notifications).createNotification(eq(admin), any(Complaint.class), anyString(), anyString());
    }

    @Test
    void urgentComplaintWithoutOfficersStaysSubmittedAndAlertsAdmin() {
        as(citizen);
        when(citizens.findById(1)).thenReturn(Optional.of(citizenProfile));
        when(users.findByUserRole(Role.ROLE_ADMIN)).thenReturn(List.of(admin));
        when(caseOfficerService.getOfficerWorkloads()).thenReturn(List.of());
        ComplaintRequestDto request = new ComplaintRequestDto();
        request.setTitle("Burst fixture");
        request.setDescription("Fixture description");
        request.setCategory("Water Pipeline Burst");
        request.setAddress("Verified fixture address");
        request.setLatitude(new BigDecimal("18.12345678"));
        request.setLongitude(new BigDecimal("73.12345678"));
        request.setImageUrl("/api/complaint-photos/12345678-1234-1234-1234-123456789abc/content");

        var response = service.createComplaint(request, citizen.getEmail());

        assertEquals(ComplaintPriority.HIGH, response.getPriority());
        assertEquals(ComplaintStatus.SUBMITTED, response.getStatus());
        assertNull(response.getOfficerId());
        verify(coordinator).lock();
        verify(history, never()).save(any());
        verify(notifications).createNotification(eq(admin), any(Complaint.class),
                eq("Unassigned HIGH-Priority Complaint"), anyString());
    }

    @Test
    void adminAssignmentUpdatesStatusHistoryAndNotifications() {
        as(admin);
        complaint.setStatus(ComplaintStatus.SUBMITTED);
        complaint.setOfficer(null);
        AssignComplaintRequestDto request = new AssignComplaintRequestDto(3, " Fixture assignment ");

        var response = service.assignComplaint(17, request, admin.getEmail());

        assertEquals(ComplaintStatus.ASSIGNED, complaint.getStatus());
        assertEquals(3, response.getOfficerId());
        ArgumentCaptor<ComplaintHistory> captor = ArgumentCaptor.forClass(ComplaintHistory.class);
        verify(history).save(captor.capture());
        assertEquals("Fixture assignment", captor.getValue().getRemarks());
        verify(notifications).createNotification(eq(citizen), eq(complaint), eq("Complaint Assigned"), anyString());
        verify(notifications).createNotification(eq(officerUser), eq(complaint), eq("New Complaint Assigned"), anyString());
        verify(coordinator).lock();
    }

    @Test
    void reassignmentNotifiesPreviousOfficer() {
        as(admin);
        complaint.setOfficer(otherOfficer);
        service.assignComplaint(17, new AssignComplaintRequestDto(3, null), admin.getEmail());
        verify(notifications).createNotification(eq(otherOfficerUser), eq(complaint), eq("Complaint Assignment Changed"), anyString());
    }

    @Test
    void nonAdminCannotAssignAndInProgressCannotBeReassigned() {
        as(citizen);
        forbidden(() -> service.assignComplaint(17, new AssignComplaintRequestDto(3, null), citizen.getEmail()));
        as(admin);
        complaint.setStatus(ComplaintStatus.IN_PROGRESS);
        assertEquals(HttpStatus.CONFLICT, assertThrows(ResponseStatusException.class,
                () -> service.assignComplaint(17, new AssignComplaintRequestDto(3, null), admin.getEmail())).getStatusCode());
        verify(complaints, never()).save(any());
    }

    @Test
    void assignedOfficerCanAdvanceStatusAndCreatesHistory() {
        as(officerUser);
        var response = service.updateComplaintStatus(17,
                new UpdateComplaintStatusRequestDto(ComplaintStatus.IN_PROGRESS, "Investigating"), officerUser.getEmail());
        assertEquals(ComplaintStatus.IN_PROGRESS, response.getStatus());
        ArgumentCaptor<ComplaintHistory> captor = ArgumentCaptor.forClass(ComplaintHistory.class);
        verify(history).save(captor.capture());
        assertEquals("Investigating", captor.getValue().getRemarks());
        verify(notifications).createNotification(eq(citizen), eq(complaint), eq("Complaint Status Updated"), anyString());
        verify(coordinator).lock();
    }

    @Test
    void unrelatedOfficerAndCitizenCannotChangeStatus() {
        as(otherOfficerUser);
        when(officers.findById(4)).thenReturn(Optional.of(otherOfficer));
        forbidden(() -> service.updateComplaintStatus(17,
                new UpdateComplaintStatusRequestDto(ComplaintStatus.IN_PROGRESS, null), otherOfficerUser.getEmail()));
        as(citizen);
        forbidden(() -> service.updateComplaintStatus(17,
                new UpdateComplaintStatusRequestDto(ComplaintStatus.IN_PROGRESS, null), citizen.getEmail()));
        verify(complaints, never()).save(any());
    }

    @Test
    void invalidOfficerTransitionIsRejected() {
        as(officerUser);
        assertEquals(HttpStatus.CONFLICT, assertThrows(ResponseStatusException.class,
                () -> service.updateComplaintStatus(17,
                        new UpdateComplaintStatusRequestDto(ComplaintStatus.RESOLVED, null), officerUser.getEmail())).getStatusCode());
        verify(complaints, never()).save(any());
    }

    @Test
    void onlyAdminCanChangePriority() {
        UpdateComplaintPriorityRequestDto request = new UpdateComplaintPriorityRequestDto();
        request.setPriority(ComplaintPriority.HIGH);
        as(citizen);
        forbidden(() -> service.updateComplaintPriority(17, request, citizen.getEmail()));
        verify(coordinator, never()).lock();
        as(admin);
        var response = service.updateComplaintPriority(17, request, admin.getEmail());
        assertEquals(ComplaintPriority.HIGH, response.getPriority());
        verify(history).save(any(ComplaintHistory.class));
        verify(notifications).createNotification(eq(citizen), eq(complaint), eq("Complaint Priority Updated"), anyString());
        verify(notifications).createNotification(eq(officerUser), eq(complaint), eq("Complaint Priority Changed"), anyString());
        var order = inOrder(coordinator, complaints);
        order.verify(coordinator).lock();
        order.verify(complaints).findById(17);
    }

    @Test
    void eligibleHighPromotionAssignsLeastLoadedAndWritesDistinctHistory() {
        as(admin);
        when(users.findByUserRole(Role.ROLE_ADMIN)).thenReturn(List.of(admin));
        complaint.setStatus(ComplaintStatus.SUBMITTED);
        complaint.setOfficer(null);
        when(caseOfficerService.getOfficerWorkloads()).thenReturn(List.of(
                new CaseOfficerWorkloadDto(4, "Fixture", "4", "CMS-OFF-4", 0, 0, 0),
                new CaseOfficerWorkloadDto(3, "Fixture", "3", "CMS-OFF-3", 0, 0, 0)));
        UpdateComplaintPriorityRequestDto request = new UpdateComplaintPriorityRequestDto();
        request.setPriority(ComplaintPriority.HIGH);

        var response = service.updateComplaintPriority(17, request, admin.getEmail());

        assertEquals(ComplaintPriority.HIGH, response.getPriority());
        assertEquals(ComplaintStatus.ASSIGNED, response.getStatus());
        assertEquals(3, response.getOfficerId());
        ArgumentCaptor<ComplaintHistory> captor = ArgumentCaptor.forClass(ComplaintHistory.class);
        verify(history, times(2)).save(captor.capture());
        assertEquals("MEDIUM", captor.getAllValues().get(0).getPreviousPriority());
        assertEquals("HIGH", captor.getAllValues().get(0).getNewPriority());
        assertNull(captor.getAllValues().get(0).getNewStatus());
        assertEquals("SUBMITTED", captor.getAllValues().get(1).getPreviousStatus());
        assertEquals("ASSIGNED", captor.getAllValues().get(1).getNewStatus());
        assertNull(captor.getAllValues().get(1).getNewPriority());
        verify(notifications, times(1)).createNotification(eq(citizen), eq(complaint), anyString(), anyString());
        verify(notifications, times(1)).createNotification(eq(officerUser), eq(complaint), anyString(), anyString());
        verify(notifications, times(1)).createNotification(eq(admin), eq(complaint), anyString(), anyString());
        var order = inOrder(coordinator, complaints, caseOfficerService);
        order.verify(coordinator).lock();
        order.verify(complaints).findById(17);
        order.verify(caseOfficerService).getOfficerWorkloads();
    }

    @Test
    void highPromotionWithoutOfficersKeepsSubmittedAndNotifiesAdmin() {
        as(admin);
        when(users.findByUserRole(Role.ROLE_ADMIN)).thenReturn(List.of(admin));
        complaint.setPriority(ComplaintPriority.LOW);
        complaint.setStatus(ComplaintStatus.SUBMITTED);
        complaint.setOfficer(null);
        when(caseOfficerService.getOfficerWorkloads()).thenReturn(List.of());
        UpdateComplaintPriorityRequestDto request = new UpdateComplaintPriorityRequestDto();
        request.setPriority(ComplaintPriority.HIGH);

        var response = service.updateComplaintPriority(17, request, admin.getEmail());

        assertEquals(ComplaintPriority.HIGH, response.getPriority());
        assertEquals(ComplaintStatus.SUBMITTED, response.getStatus());
        assertNull(response.getOfficerId());
        verify(history, times(1)).save(any(ComplaintHistory.class));
        verify(notifications).createNotification(eq(admin), eq(complaint),
                eq("Unassigned HIGH-Priority Complaint"), anyString());
        verify(notifications, times(1)).createNotification(eq(citizen), eq(complaint), anyString(), anyString());
    }

    @Test
    void highPriorityChangePreservesExistingOfficerAndDoesNotDuplicateNoOpEvents() {
        as(admin);
        UpdateComplaintPriorityRequestDto request = new UpdateComplaintPriorityRequestDto();
        request.setPriority(ComplaintPriority.HIGH);
        var changed = service.updateComplaintPriority(17, request, admin.getEmail());
        assertEquals(3, changed.getOfficerId());
        assertEquals(ComplaintStatus.ASSIGNED, changed.getStatus());
        verify(caseOfficerService, never()).getOfficerWorkloads();
        verify(history, times(1)).save(any(ComplaintHistory.class));

        service.updateComplaintPriority(17, request, admin.getEmail());
        verify(complaints, times(1)).save(any(Complaint.class));
        verify(history, times(1)).save(any(ComplaintHistory.class));
        verify(notifications, times(1)).createNotification(eq(citizen), any(Complaint.class), anyString(), anyString());
        verify(notifications, times(1)).createNotification(eq(officerUser), any(Complaint.class), anyString(), anyString());
    }

    @Test
    void ineligibleUnassignedComplaintsAreNeverAutoAssignedOnPromotion() {
        as(admin);
        complaint.setOfficer(null);
        UpdateComplaintPriorityRequestDto request = new UpdateComplaintPriorityRequestDto();
        request.setPriority(ComplaintPriority.HIGH);
        for (ComplaintStatus status : new ComplaintStatus[] {
                ComplaintStatus.ASSIGNED, ComplaintStatus.IN_PROGRESS,
                ComplaintStatus.RESOLVED, ComplaintStatus.REJECTED }) {
            complaint.setPriority(ComplaintPriority.MEDIUM);
            complaint.setStatus(status);
            var response = service.updateComplaintPriority(17, request, admin.getEmail());
            assertEquals(status, response.getStatus());
            assertNull(response.getOfficerId());
        }
        verify(caseOfficerService, never()).getOfficerWorkloads();
        verify(history, times(4)).save(any(ComplaintHistory.class));
    }
}
