package com.cmsbackend.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import com.cmsbackend.dtos.ComplaintRequestDto;
import com.cmsbackend.dtos.ComplaintResponseDto;
import com.cmsbackend.entities.CaseOfficer;
import com.cmsbackend.entities.Citizen;
import com.cmsbackend.entities.ComplaintPhotoUpload;
import com.cmsbackend.entities.User;
import com.cmsbackend.enums.ComplaintPriority;
import com.cmsbackend.enums.ComplaintStatus;
import com.cmsbackend.enums.Role;
import com.cmsbackend.repositories.CaseOfficerRepository;
import com.cmsbackend.repositories.CitizenRepository;
import com.cmsbackend.repositories.ComplaintPhotoUploadRepository;
import com.cmsbackend.repositories.UserRepository;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(properties = {
    "server.ssl.enabled=false",
    "server.ssl.certificate=",
    "server.ssl.certificate-private-key=",
    "spring.datasource.url=jdbc:h2:mem:cms_auto_assignment_concurrency;MODE=MySQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.show-sql=false",
    "spring.app.jwtSecret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
})
class ConcurrentAutoAssignmentTest {
    @Autowired private ComplaintService complaintService;
    @Autowired private UserRepository users;
    @Autowired private CitizenRepository citizens;
    @Autowired private CaseOfficerRepository officers;
    @Autowired private ComplaintPhotoUploadRepository uploads;

    private User user(Role role, String email) {
        User user = new User();
        user.setFirstName("Safe");
        user.setLastName("Fixture");
        user.setEmail(email);
        user.setPassword("unused-fixture-password");
        user.setUserRole(role);
        return users.saveAndFlush(user);
    }

    private ComplaintRequestDto urgentRequest(String title, User owner) {
        UUID uploadId = UUID.randomUUID();
        ComplaintPhotoUpload upload = new ComplaintPhotoUpload();
        upload.setUploadId(uploadId);
        upload.setUser(owner);
        upload.setStorageKey(uploadId + ".jpg");
        upload.setMimeType("image/jpeg");
        upload.setFileSize(100L);
        upload.setLatitude(new BigDecimal("18.52000000"));
        upload.setLongitude(new BigDecimal("73.85000000"));
        upload.setAccuracyMeters(new BigDecimal("10.00"));
        upload.setExpiresAt(LocalDateTime.now().plusDays(1));
        uploads.saveAndFlush(upload);
        ComplaintRequestDto request = new ComplaintRequestDto();
        request.setTitle(title);
        request.setDescription("Isolated concurrency fixture");
        request.setCategory("Gas Leakage");
        request.setAddress("In-memory address");
        request.setLatitude(upload.getLatitude());
        request.setLongitude(upload.getLongitude());
        request.setImageUrl("/api/complaint-photos/" + uploadId + "/content");
        return request;
    }

    @Test
    void concurrentHighSubmissionsSeeCommittedWorkloadChanges() throws Exception {
        User citizenUser = user(Role.ROLE_CITIZEN, "concurrent-citizen@example.test");
        Citizen citizen = new Citizen();
        citizen.setUser(citizenUser);
        citizens.saveAndFlush(citizen);
        User firstUser = user(Role.ROLE_OFFICER, "concurrent-officer-one@example.test");
        User secondUser = user(Role.ROLE_OFFICER, "concurrent-officer-two@example.test");
        for (User officerUser : new User[] { firstUser, secondUser }) {
            CaseOfficer officer = new CaseOfficer();
            officer.setUser(officerUser);
            officer.setEmployeeNumber("CMS-OFF-" + officerUser.getUserId());
            officers.saveAndFlush(officer);
        }
        ComplaintRequestDto firstRequest = urgentRequest("Concurrent fixture one", citizenUser);
        ComplaintRequestDto secondRequest = urgentRequest("Concurrent fixture two", citizenUser);

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        try {
            var first = executor.submit(() -> {
                ready.countDown();
                start.await();
                return complaintService.createComplaint(firstRequest, citizenUser.getEmail());
            });
            var second = executor.submit(() -> {
                ready.countDown();
                start.await();
                return complaintService.createComplaint(secondRequest, citizenUser.getEmail());
            });
            if (!ready.await(5, TimeUnit.SECONDS)) throw new AssertionError("Workers did not become ready");
            start.countDown();
            ComplaintResponseDto firstResult = first.get(20, TimeUnit.SECONDS);
            ComplaintResponseDto secondResult = second.get(20, TimeUnit.SECONDS);
            assertEquals(ComplaintPriority.HIGH, firstResult.getPriority());
            assertEquals(ComplaintPriority.HIGH, secondResult.getPriority());
            assertEquals(ComplaintStatus.ASSIGNED, firstResult.getStatus());
            assertEquals(ComplaintStatus.ASSIGNED, secondResult.getStatus());
            assertNotEquals(firstResult.getOfficerId(), secondResult.getOfficerId());
        } finally {
            start.countDown();
            executor.shutdownNow();
        }
    }
}
