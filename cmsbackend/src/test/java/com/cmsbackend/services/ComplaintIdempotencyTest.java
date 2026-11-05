package com.cmsbackend.services;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.web.server.ResponseStatusException;
import com.cmsbackend.dtos.ComplaintRequestDto;
import com.cmsbackend.dtos.ComplaintResponseDto;
import com.cmsbackend.entities.Citizen;
import com.cmsbackend.entities.CaseOfficer;
import com.cmsbackend.entities.ComplaintPhotoUpload;
import com.cmsbackend.entities.User;
import com.cmsbackend.enums.Role;
import com.cmsbackend.repositories.*;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(properties = {
    "server.ssl.enabled=false", "server.ssl.certificate=", "server.ssl.certificate-private-key=",
    "spring.datasource.url=jdbc:h2:mem:cms_idempotency;MODE=MySQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa",
    "spring.datasource.password=", "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.show-sql=false",
    "spring.app.jwtSecret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
})
class ComplaintIdempotencyTest {
    @Autowired private ComplaintService complaints;
    @Autowired private ComplaintRepository complaintRepository;
    @Autowired private ComplaintPhotoUploadRepository uploads;
    @Autowired private UserRepository users;
    @Autowired private CitizenRepository citizens;
    @Autowired private CaseOfficerRepository officers;
    @Autowired private NotificationRepository notifications;
    @Autowired private ComplaintHistoryRepository history;

    private User user(Role role) {
        User user = new User();
        user.setFirstName("Safe");
        user.setLastName("Fixture");
        user.setEmail(UUID.randomUUID() + "@example.test");
        user.setPassword("unused-fixture-password");
        user.setUserRole(role);
        return users.saveAndFlush(user);
    }

    private User citizen() {
        User user = user(Role.ROLE_CITIZEN);
        Citizen profile = new Citizen();
        profile.setUser(user);
        citizens.saveAndFlush(profile);
        return user;
    }

    private ComplaintRequestDto request(User owner) {
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
        request.setTitle("Safe fixture issue");
        request.setDescription("Isolated idempotency fixture");
        request.setCategory("Gas Leakage");
        request.setAddress("Verified fixture address");
        request.setLatitude(upload.getLatitude());
        request.setLongitude(upload.getLongitude());
        request.setImageUrl("/api/complaint-photos/" + uploadId + "/content");
        return request;
    }

    private ComplaintPhotoUpload uploadFor(ComplaintRequestDto request) {
        String reference = request.getImageUrl();
        UUID id = UUID.fromString(reference.substring(
                "/api/complaint-photos/".length(), reference.length() - "/content".length()));
        return uploads.findById(id).orElseThrow();
    }

    private void rejected(HttpStatus status, ComplaintRequestDto request, User owner) {
        long before = complaintRepository.count();
        assertEquals(status, assertThrows(ResponseStatusException.class,
                () -> complaints.createComplaint(request, owner.getEmail())).getStatusCode());
        assertEquals(before, complaintRepository.count());
    }

    @Test
    void missingAndInvalidReferencesAndBlankAddressAreRejected() {
        User owner = citizen();
        ComplaintRequestDto request = request(owner);
        request.setImageUrl(null);
        rejected(HttpStatus.BAD_REQUEST, request, owner);
        request.setImageUrl("https://example.test/photo.jpg");
        rejected(HttpStatus.BAD_REQUEST, request, owner);
        request.setImageUrl("/api/complaint-photos/" + UUID.randomUUID() + "/content");
        rejected(HttpStatus.BAD_REQUEST, request, owner);
        request = request(owner);
        request.setAddress("  ");
        rejected(HttpStatus.BAD_REQUEST, request, owner);
    }

    @Test
    void missingInvalidAndMismatchedCoordinatesAreRejected() {
        User owner = citizen();
        ComplaintRequestDto request = request(owner);
        request.setLatitude(null);
        rejected(HttpStatus.BAD_REQUEST, request, owner);
        request.setLatitude(new BigDecimal("91"));
        rejected(HttpStatus.BAD_REQUEST, request, owner);
        request.setLatitude(new BigDecimal("18.52000001"));
        rejected(HttpStatus.BAD_REQUEST, request, owner);
        request = request(owner);
        request.setLongitude(null);
        rejected(HttpStatus.BAD_REQUEST, request, owner);
    }

    @Test
    void invalidStoredMetadataAndExpiredUnusedUploadAreRejected() {
        User owner = citizen();
        ComplaintRequestDto request = request(owner);
        ComplaintPhotoUpload upload = uploadFor(request);
        upload.setAccuracyMeters(new BigDecimal("51"));
        uploads.saveAndFlush(upload);
        rejected(HttpStatus.BAD_REQUEST, request, owner);

        upload.setAccuracyMeters(new BigDecimal("10"));
        upload.setLatitude(new BigDecimal("95"));
        uploads.saveAndFlush(upload);
        rejected(HttpStatus.BAD_REQUEST, request, owner);

        upload.setLatitude(new BigDecimal("18.52000000"));
        upload.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        uploads.saveAndFlush(upload);
        rejected(HttpStatus.CONFLICT, request, owner);
    }

    @Test
    void spentUploadWithoutComplaintCannotBeReused() {
        User owner = citizen();
        ComplaintRequestDto request = request(owner);
        ComplaintPhotoUpload upload = uploadFor(request);
        upload.setConsumedAt(LocalDateTime.now());
        uploads.saveAndFlush(upload);
        rejected(HttpStatus.CONFLICT, request, owner);
    }

    @Test
    void successfulRetryReturnsOriginalWithoutRepeatingHistoryOrNotifications() {
        User owner = citizen();
        user(Role.ROLE_ADMIN);
        User officerUser = user(Role.ROLE_OFFICER);
        CaseOfficer officer = new CaseOfficer();
        officer.setUser(officerUser);
        officer.setEmployeeNumber("CMS-OFF-" + officerUser.getUserId());
        officers.saveAndFlush(officer);
        ComplaintRequestDto request = request(owner);
        long historyBefore = history.count();
        long notificationsBefore = notifications.count();
        int adminCount = users.findByUserRole(Role.ROLE_ADMIN).size();
        ComplaintResponseDto first = complaints.createComplaint(request, owner.getEmail());
        assertEquals(request.getLatitude(), first.getLatitude());
        assertEquals(request.getLongitude(), first.getLongitude());
        ComplaintPhotoUpload usedUpload = uploadFor(request);
        assertEquals(first.getComplaintId(), usedUpload.getComplaint().getComplaintId());
        assertNotNull(usedUpload.getConsumedAt());
        usedUpload.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        uploads.saveAndFlush(usedUpload);
        long complaintCount = complaintRepository.count();
        long historyCount = history.count();
        long notificationCount = notifications.count();
        assertEquals(historyBefore + 1, historyCount);
        assertEquals(notificationsBefore + 2 + adminCount, notificationCount);
        ComplaintResponseDto retry = complaints.createComplaint(request, owner.getEmail());
        assertEquals(first.getComplaintId(), retry.getComplaintId());
        assertEquals(complaintCount, complaintRepository.count());
        assertEquals(historyCount, history.count());
        assertEquals(notificationCount, notifications.count());
    }

    @Test
    void concurrentRetryCreatesOneComplaint() throws Exception {
        User owner = citizen();
        ComplaintRequestDto request = request(owner);
        long before = complaintRepository.count();
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        try {
            var first = executor.submit(() -> {
                ready.countDown(); start.await();
                return complaints.createComplaint(request, owner.getEmail());
            });
            var second = executor.submit(() -> {
                ready.countDown(); start.await();
                return complaints.createComplaint(request, owner.getEmail());
            });
            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();
            assertEquals(first.get(20, TimeUnit.SECONDS).getComplaintId(),
                    second.get(20, TimeUnit.SECONDS).getComplaintId());
            assertEquals(before + 1, complaintRepository.count());
        } finally {
            start.countDown(); executor.shutdownNow();
        }
    }

    @Test
    void changedDetailsCannotReuseUpload() {
        User owner = citizen();
        ComplaintRequestDto request = request(owner);
        complaints.createComplaint(request, owner.getEmail());
        long before = complaintRepository.count();
        request.setDescription("Different details");
        assertEquals(HttpStatus.CONFLICT, assertThrows(ResponseStatusException.class,
                () -> complaints.createComplaint(request, owner.getEmail())).getStatusCode());
        assertEquals(before, complaintRepository.count());
    }

    @Test
    void anotherCitizenAndNonCitizenCannotReuseUpload() {
        User owner = citizen();
        User other = citizen();
        User admin = user(Role.ROLE_ADMIN);
        ComplaintRequestDto request = request(owner);
        complaints.createComplaint(request, owner.getEmail());
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ResponseStatusException.class,
                () -> complaints.createComplaint(request, other.getEmail())).getStatusCode());
        assertEquals(HttpStatus.FORBIDDEN, assertThrows(ResponseStatusException.class,
                () -> complaints.createComplaint(request, admin.getEmail())).getStatusCode());
    }
}
