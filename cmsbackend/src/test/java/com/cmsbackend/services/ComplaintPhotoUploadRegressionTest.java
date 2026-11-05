package com.cmsbackend.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;
import com.cmsbackend.entities.Complaint;
import com.cmsbackend.entities.ComplaintPhotoUpload;
import com.cmsbackend.entities.User;
import com.cmsbackend.enums.Role;
import com.cmsbackend.repositories.ComplaintPhotoUploadRepository;
import com.cmsbackend.repositories.UserRepository;
import com.cmsbackend.services.impl.ComplaintPhotoServiceImpl;

class ComplaintPhotoUploadRegressionTest {
    private final ComplaintPhotoUploadRepository uploads = mock(ComplaintPhotoUploadRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final ComplaintPhotoServiceImpl service = new ComplaintPhotoServiceImpl(uploads, users,
            "unused-test-photo-storage", 5_242_880L, new BigDecimal("50"));
    private final UUID uploadId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
    private final String reference = "/api/complaint-photos/" + uploadId + "/content";
    private final BigDecimal latitude = new BigDecimal("18.52000000");
    private final BigDecimal longitude = new BigDecimal("73.85000000");

    private User citizen(int id) {
        User user = new User();
        user.setUserId(id);
        user.setEmail("fixture" + id + "@example.test");
        user.setUserRole(Role.ROLE_CITIZEN);
        when(users.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        return user;
    }

    private ComplaintPhotoUpload available(User owner) {
        ComplaintPhotoUpload upload = new ComplaintPhotoUpload();
        upload.setUploadId(uploadId);
        upload.setUser(owner);
        upload.setStorageKey(uploadId + ".jpg");
        upload.setMimeType("image/jpeg");
        upload.setFileSize(100L);
        upload.setLatitude(latitude);
        upload.setLongitude(longitude);
        upload.setAccuracyMeters(new BigDecimal("10"));
        upload.setExpiresAt(LocalDateTime.now().plusHours(1));
        when(uploads.findByUploadIdAndUser_UserId(uploadId, owner.getUserId())).thenReturn(Optional.of(upload));
        return upload;
    }

    private void status(HttpStatus expected, Runnable action) {
        assertEquals(expected, assertThrows(ResponseStatusException.class, action::run).getStatusCode());
    }

    @Test
    void ownerCanConsumePhotoExactlyOnceForMatchingCoordinates() {
        User owner = citizen(1);
        ComplaintPhotoUpload upload = available(owner);
        Complaint complaint = new Complaint();
        service.consumeForComplaint(reference, complaint, latitude, longitude, owner.getEmail());
        assertSame(complaint, upload.getComplaint());
        assertNotNull(upload.getConsumedAt());
        status(HttpStatus.CONFLICT, () -> service.consumeForComplaint(reference, new Complaint(), latitude, longitude, owner.getEmail()));
    }

    @Test
    void otherCitizenCannotConsumeAnotherOwnersPhoto() {
        citizen(1);
        User other = citizen(2);
        status(HttpStatus.BAD_REQUEST, () -> service.consumeForComplaint(reference, new Complaint(), latitude, longitude, other.getEmail()));
        verify(uploads).findByUploadIdAndUser_UserId(uploadId, 2);
    }

    @Test
    void locationMismatchAndExpiryRejectConsumption() {
        User owner = citizen(1);
        ComplaintPhotoUpload upload = available(owner);
        status(HttpStatus.BAD_REQUEST, () -> service.consumeForComplaint(reference, new Complaint(), BigDecimal.ZERO, longitude, owner.getEmail()));
        upload.setExpiresAt(LocalDateTime.now().minusSeconds(1));
        status(HttpStatus.CONFLICT, () -> service.consumeForComplaint(reference, new Complaint(), latitude, longitude, owner.getEmail()));
        assertNull(upload.getComplaint());
    }

    @Test
    void malformedReferenceAndNonCitizenAreRejected() {
        User owner = citizen(1);
        status(HttpStatus.BAD_REQUEST, () -> service.consumeForComplaint("https://external.test/image.jpg", new Complaint(), latitude, longitude, owner.getEmail()));
        User admin = new User();
        admin.setUserId(2);
        admin.setEmail("admin@example.test");
        admin.setUserRole(Role.ROLE_ADMIN);
        when(users.findByEmail(admin.getEmail())).thenReturn(Optional.of(admin));
        status(HttpStatus.FORBIDDEN, () -> service.consumeForComplaint(reference, new Complaint(), latitude, longitude, admin.getEmail()));
    }

    @Test
    void uploadRejectsInaccurateLocationAndInvalidImageBeforeStorage() {
        User owner = citizen(1);
        MockMultipartFile fakeImage = new MockMultipartFile("photo", "fake.jpg", "image/jpeg", "not a valid image".getBytes());
        status(HttpStatus.BAD_REQUEST, () -> service.upload(fakeImage, latitude, longitude, new BigDecimal("51"), owner.getEmail()));
        status(HttpStatus.BAD_REQUEST, () -> service.upload(fakeImage, latitude, longitude, new BigDecimal("12"), owner.getEmail()));
        verify(uploads, never()).save(any());
    }
}
