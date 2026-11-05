package com.cmsbackend.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import com.cmsbackend.entities.CaseOfficer;
import com.cmsbackend.entities.Complaint;
import com.cmsbackend.entities.ComplaintPhotoUpload;
import com.cmsbackend.entities.User;
import com.cmsbackend.enums.Role;
import com.cmsbackend.repositories.ComplaintPhotoUploadRepository;
import com.cmsbackend.repositories.UserRepository;
import com.cmsbackend.services.impl.ComplaintPhotoServiceImpl;

class ComplaintPhotoAuthorizationTest {
    private final UUID uploadId = UUID.randomUUID();
    private final ComplaintPhotoUploadRepository uploads = mock(ComplaintPhotoUploadRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final ComplaintPhotoServiceImpl service = new ComplaintPhotoServiceImpl(uploads, users, "unused-test-storage", 5242880, BigDecimal.valueOf(50));

    private User account(int id, Role role) {
        User user = new User();
        user.setUserId(id);
        user.setUserRole(role);
        return user;
    }

    private void prepare(boolean associated, User requester) {
        ComplaintPhotoUpload upload = new ComplaintPhotoUpload();
        upload.setUser(account(1, Role.ROLE_CITIZEN));
        upload.setMimeType("image/jpeg");
        if (associated) {
            CaseOfficer officer = new CaseOfficer();
            officer.setUserId(3);
            Complaint complaint = new Complaint();
            complaint.setOfficer(officer);
            upload.setComplaint(complaint);
        }
        when(uploads.findById(uploadId)).thenReturn(Optional.of(upload));
        when(users.findByEmail("requester@example.test")).thenReturn(Optional.of(requester));
    }

    @Test
    void draftPhotoIsOwnerOnlyIncludingAgainstAdministrators() {
        prepare(false, account(1, Role.ROLE_CITIZEN));
        assertEquals("image/jpeg", service.getContentType(uploadId, "requester@example.test"));
        for (User requester : new User[] { account(2, Role.ROLE_ADMIN), account(3, Role.ROLE_OFFICER), account(4, Role.ROLE_CITIZEN) }) {
            prepare(false, requester);
            assertEquals(HttpStatus.FORBIDDEN, assertThrows(ResponseStatusException.class,
                    () -> service.getContentType(uploadId, "requester@example.test")).getStatusCode());
        }
    }

    @Test
    void submittedPhotoAllowsOwnerAdminAndAssignedOfficerOnly() {
        for (User requester : new User[] { account(1, Role.ROLE_CITIZEN), account(2, Role.ROLE_ADMIN), account(3, Role.ROLE_OFFICER) }) {
            prepare(true, requester);
            assertEquals("image/jpeg", service.getContentType(uploadId, "requester@example.test"));
        }
        for (User requester : new User[] { account(4, Role.ROLE_CITIZEN), account(5, Role.ROLE_OFFICER) }) {
            prepare(true, requester);
            assertEquals(HttpStatus.FORBIDDEN, assertThrows(ResponseStatusException.class,
                    () -> service.getContentType(uploadId, "requester@example.test")).getStatusCode());
        }
    }
}
