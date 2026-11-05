package com.cmsbackend.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import com.cmsbackend.entities.*;
import com.cmsbackend.enums.ComplaintPriority;
import com.cmsbackend.enums.ComplaintStatus;
import com.cmsbackend.enums.Role;
import com.cmsbackend.repositories.*;
import com.cmsbackend.services.impl.ComplaintServiceImpl;

class ComplaintReadAuthorizationTest {
    private final ComplaintRepository complaints = mock(ComplaintRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final CitizenRepository citizens = mock(CitizenRepository.class);
    private final CaseOfficerRepository officers = mock(CaseOfficerRepository.class);
    private final ModelMapper mapper = spy(new ModelMapper());
    private final ComplaintServiceImpl service = new ComplaintServiceImpl();
    private final Complaint complaint;

    ComplaintReadAuthorizationTest() {
        ReflectionTestUtils.setField(service, "complaintRepository", complaints);
        ReflectionTestUtils.setField(service, "userRepository", users);
        ReflectionTestUtils.setField(service, "citizenRepository", citizens);
        ReflectionTestUtils.setField(service, "caseOfficerRepository", officers);
        ReflectionTestUtils.setField(service, "modelMapper", mapper);
        Citizen citizen = new Citizen();
        citizen.setUserId(1);
        citizen.setUser(account(1, Role.ROLE_CITIZEN));
        CaseOfficer officer = new CaseOfficer();
        officer.setUserId(3);
        officer.setUser(account(3, Role.ROLE_OFFICER));
        officer.setEmployeeNumber("CMS-OFF-003");
        complaint = new Complaint();
        complaint.setComplaintId(17);
        complaint.setCitizen(citizen);
        complaint.setOfficer(officer);
        complaint.setTitle("Reported issue");
        complaint.setAddress("Existing verified address");
        complaint.setLatitude(new BigDecimal("18.12345678"));
        complaint.setLongitude(new BigDecimal("73.12345678"));
        complaint.setImageUrl("/api/complaint-photos/12345678-1234-1234-1234-123456789abc/content");
        complaint.setStatus(ComplaintStatus.ASSIGNED);
        complaint.setPriority(ComplaintPriority.MEDIUM);
        when(complaints.findById(17)).thenReturn(Optional.of(complaint));
    }

    private User account(int id, Role role) {
        User user = new User();
        user.setUserId(id);
        user.setUserRole(role);
        user.setFirstName("Account");
        user.setLastName(String.valueOf(id));
        user.setEmail("account" + id + "@example.test");
        return user;
    }

    private void authenticate(int id, Role role) {
        when(users.findByEmail("caller@example.test")).thenReturn(Optional.of(account(id, role)));
    }

    private void assertHidden() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.getComplaint(17, "caller@example.test"));
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("Complaint not found", exception.getReason());
        verifyNoInteractions(mapper);
    }

    @Test
    void citizenCanReadOwnComplaintWithManagedEvidenceAndOfficerInformation() {
        authenticate(1, Role.ROLE_CITIZEN);
        var response = service.getComplaint(17, "caller@example.test");
        assertEquals(complaint.getImageUrl(), response.getImageUrl());
        assertEquals(complaint.getLatitude(), response.getLatitude());
        assertEquals(complaint.getLongitude(), response.getLongitude());
        assertEquals("Account 3", response.getOfficerName());
        assertEquals("CMS-OFF-003", response.getOfficerEmployeeNumber());
        assertEquals("account1@example.test", response.getCitizenEmail());
    }

    @Test
    void citizenCannotReadAnotherCitizensComplaint() {
        authenticate(4, Role.ROLE_CITIZEN);
        assertHidden();
    }

    @Test
    void officerCanReadAssignedComplaintWithoutUnnecessaryCitizenContactData() {
        authenticate(3, Role.ROLE_OFFICER);
        var response = service.getComplaint(17, "caller@example.test");
        assertEquals("Account 1", response.getCitizenName());
        assertEquals(3, response.getOfficerId());
        assertNull(response.getCitizenEmail());
    }

    @Test
    void unrelatedOfficerCannotReadTheComplaint() {
        authenticate(5, Role.ROLE_OFFICER);
        assertHidden();
    }

    @Test
    void officerCannotReadAnUnassignedComplaint() {
        authenticate(3, Role.ROLE_OFFICER);
        complaint.setOfficer(null);
        assertHidden();
    }

    @Test
    void previousOfficerLosesReadAccessAfterReassignment() {
        authenticate(3, Role.ROLE_OFFICER);
        complaint.getOfficer().setUserId(5);
        assertHidden();
    }

    @Test
    void administratorCanReadAnyComplaintIncludingUnassigned() {
        authenticate(2, Role.ROLE_ADMIN);
        complaint.setOfficer(null);
        var response = service.getComplaint(17, "caller@example.test");
        assertEquals("account1@example.test", response.getCitizenEmail());
        assertNull(response.getOfficerId());
        assertNull(response.getOfficerName());
    }

    @Test
    void missingRecordReturnsNotFound() {
        authenticate(1, Role.ROLE_CITIZEN);
        when(complaints.findById(17)).thenReturn(Optional.empty());
        assertHidden();
    }

    @Test
    void existingCitizenListQueryIsOwnerScoped() {
        authenticate(1, Role.ROLE_CITIZEN);
        when(citizens.findById(1)).thenReturn(Optional.of(complaint.getCitizen()));
        when(complaints.findByCitizen_UserIdOrderByCreatedAtDesc(1)).thenReturn(List.of());
        assertTrue(service.getMyComplaints("caller@example.test").isEmpty());
        verify(complaints).findByCitizen_UserIdOrderByCreatedAtDesc(1);
        verify(complaints, never()).findWithOptionalFilters(any(), any());
    }

    @Test
    void existingOfficerListQueryIsAssignmentScoped() {
        authenticate(3, Role.ROLE_OFFICER);
        when(officers.findById(3)).thenReturn(Optional.of(complaint.getOfficer()));
        when(complaints.findByOfficer_UserIdOrderByCreatedAtDesc(3)).thenReturn(List.of());
        assertTrue(service.getAssignedComplaints("caller@example.test").isEmpty());
        verify(complaints).findByOfficer_UserIdOrderByCreatedAtDesc(3);
        verify(complaints, never()).findWithOptionalFilters(any(), any());
    }

    @Test
    void onlyAdministratorCanUseTheExistingAllComplaintsService() {
        authenticate(1, Role.ROLE_CITIZEN);
        assertEquals(HttpStatus.FORBIDDEN, assertThrows(ResponseStatusException.class,
                () -> service.getAllComplaints("caller@example.test", null, null)).getStatusCode());
        authenticate(3, Role.ROLE_OFFICER);
        assertEquals(HttpStatus.FORBIDDEN, assertThrows(ResponseStatusException.class,
                () -> service.getAllComplaints("caller@example.test", null, null)).getStatusCode());
        verify(complaints, never()).findWithOptionalFilters(any(), any());
        authenticate(2, Role.ROLE_ADMIN);
        when(complaints.findWithOptionalFilters(null, null)).thenReturn(List.of());
        assertTrue(service.getAllComplaints("caller@example.test", null, null).isEmpty());
        verify(complaints).findWithOptionalFilters(null, null);
    }
}
