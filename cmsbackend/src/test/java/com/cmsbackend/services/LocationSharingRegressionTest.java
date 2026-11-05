package com.cmsbackend.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import com.cmsbackend.entities.LocationSharingSession;
import com.cmsbackend.entities.User;
import com.cmsbackend.enums.LocationSharingStatus;
import com.cmsbackend.enums.Role;
import com.cmsbackend.repositories.LocationSharingSessionRepository;
import com.cmsbackend.repositories.UserRepository;
import com.cmsbackend.services.impl.LocationSharingServiceImpl;

class LocationSharingRegressionTest {
    private final LocationSharingSessionRepository sessions = mock(LocationSharingSessionRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final LocationSharingServiceImpl service = new LocationSharingServiceImpl(sessions, users,
            "https://localhost:5173", 5, new BigDecimal("50"));

    private User user(int id, Role role) {
        User user = new User();
        user.setUserId(id);
        user.setUserRole(role);
        user.setEmail("fixture" + id + "@example.test");
        when(users.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        return user;
    }

    @Test
    void createStoresOnlyHashAndCompletionIsSingleUse() {
        User citizen = user(1, Role.ROLE_CITIZEN);
        var created = service.create(citizen.getEmail());
        ArgumentCaptor<LocationSharingSession> captor = ArgumentCaptor.forClass(LocationSharingSession.class);
        verify(sessions).save(captor.capture());
        LocationSharingSession session = captor.getValue();
        assertEquals(LocationSharingStatus.PENDING, session.getStatus());
        assertTrue(created.getShareUrl().startsWith("https://localhost:5173/share-location/"));
        assertEquals(64, session.getTokenHash().length());
        assertFalse(created.getShareUrl().contains(session.getTokenHash()));
        when(sessions.findByTokenHashForUpdate(session.getTokenHash())).thenReturn(Optional.of(session));
        String token = created.getShareUrl().substring(created.getShareUrl().lastIndexOf('/') + 1);

        service.submit(token, new BigDecimal("18.12"), new BigDecimal("73.12"), new BigDecimal("12"));
        assertEquals(LocationSharingStatus.COMPLETED, session.getStatus());
        assertEquals(new BigDecimal("18.12"), session.getLatitude());
        assertEquals(HttpStatus.GONE, assertThrows(ResponseStatusException.class,
                () -> service.submit(token, new BigDecimal("18.12"), new BigDecimal("73.12"), new BigDecimal("12"))).getStatusCode());
    }

    @Test
    void ownerCanReadButOtherCitizenCannot() {
        User citizen = user(1, Role.ROLE_CITIZEN);
        User other = user(2, Role.ROLE_CITIZEN);
        LocationSharingSession session = new LocationSharingSession();
        session.setSessionId(UUID.randomUUID());
        session.setStatus(LocationSharingStatus.PENDING);
        session.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        when(sessions.findBySessionIdAndUser_UserId(session.getSessionId(), 1)).thenReturn(Optional.of(session));

        assertEquals(LocationSharingStatus.PENDING, service.get(session.getSessionId(), citizen.getEmail()).getStatus());
        assertEquals(HttpStatus.NOT_FOUND, assertThrows(ResponseStatusException.class,
                () -> service.get(session.getSessionId(), other.getEmail())).getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND, assertThrows(ResponseStatusException.class,
                () -> service.cancel(session.getSessionId(), other.getEmail())).getStatusCode());
    }

    @Test
    void invalidAccuracyAndExpiredSessionsCannotComplete() {
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ResponseStatusException.class,
                () -> service.submit("fixture", new BigDecimal("18"), new BigDecimal("73"), new BigDecimal("51"))).getStatusCode());
        verify(sessions, never()).findByTokenHashForUpdate(anyString());
        User citizen = user(1, Role.ROLE_CITIZEN);
        var created = service.create(citizen.getEmail());
        ArgumentCaptor<LocationSharingSession> captor = ArgumentCaptor.forClass(LocationSharingSession.class);
        verify(sessions).save(captor.capture());
        LocationSharingSession session = captor.getValue();
        session.setExpiresAt(LocalDateTime.now().minusSeconds(1));
        when(sessions.findByTokenHashForUpdate(session.getTokenHash())).thenReturn(Optional.of(session));
        String token = created.getShareUrl().substring(created.getShareUrl().lastIndexOf('/') + 1);
        assertEquals(HttpStatus.GONE, assertThrows(ResponseStatusException.class,
                () -> service.submit(token, new BigDecimal("18"), new BigDecimal("73"), new BigDecimal("10"))).getStatusCode());
        assertEquals(LocationSharingStatus.EXPIRED, session.getStatus());
    }

    @Test
    void nonCitizenCannotCreateSession() {
        User admin = user(2, Role.ROLE_ADMIN);
        assertEquals(HttpStatus.FORBIDDEN, assertThrows(ResponseStatusException.class,
                () -> service.create(admin.getEmail())).getStatusCode());
        verify(sessions, never()).save(any());
    }
}
