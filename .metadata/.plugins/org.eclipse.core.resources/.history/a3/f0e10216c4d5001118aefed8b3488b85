package com.cmsbackend.services.impl;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.cmsbackend.dtos.LocationSharingCreateResponseDto;
import com.cmsbackend.dtos.LocationSharingSessionResponseDto;
import com.cmsbackend.entities.LocationSharingSession;
import com.cmsbackend.entities.User;
import com.cmsbackend.enums.LocationSharingStatus;
import com.cmsbackend.enums.Role;
import com.cmsbackend.repositories.LocationSharingSessionRepository;
import com.cmsbackend.repositories.UserRepository;
import com.cmsbackend.services.LocationSharingService;

@Service
public class LocationSharingServiceImpl implements LocationSharingService {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private final LocationSharingSessionRepository sessionRepository;
    private final UserRepository userRepository;
    private final String frontendBaseUrl;
    private final long expiryMinutes;
    private final BigDecimal maxAccuracy;

    public LocationSharingServiceImpl(LocationSharingSessionRepository sessionRepository, UserRepository userRepository,
            @Value("${cms.location-sharing.frontend-base-url:http://localhost:5173}") String frontendBaseUrl,
            @Value("${cms.location-sharing.expiry-minutes:5}") long expiryMinutes,
            @Value("${cms.photo.max-accuracy-meters:50}") BigDecimal maxAccuracy) {
        this.sessionRepository = sessionRepository;
        this.userRepository = userRepository;
        this.frontendBaseUrl = frontendBaseUrl.replaceAll("/+$", "");
        this.expiryMinutes = expiryMinutes;
        this.maxAccuracy = maxAccuracy;
    }

    @Override
    @Transactional
    public LocationSharingCreateResponseDto create(String email) {
        User user = citizen(email);
        byte[] tokenBytes = new byte[32];
        SECURE_RANDOM.nextBytes(tokenBytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(expiryMinutes);
        LocationSharingSession session = new LocationSharingSession();
        session.setSessionId(UUID.randomUUID());
        session.setUser(user);
        session.setTokenHash(hash(token));
        session.setStatus(LocationSharingStatus.PENDING);
        session.setExpiresAt(expiresAt);
        sessionRepository.save(session);
        return new LocationSharingCreateResponseDto(session.getSessionId(), frontendBaseUrl + "/share-location/" + token, expiresAt);
    }

    @Override
    @Transactional
    public LocationSharingSessionResponseDto get(UUID sessionId, String email) {
        User user = citizen(email);
        LocationSharingSession session = ownerSession(sessionId, user);
        expireIfNeeded(session);
        return response(session);
    }

    @Override
    @Transactional
    public void submit(String token, BigDecimal latitude, BigDecimal longitude, BigDecimal accuracy) {
        validateLocation(latitude, longitude, accuracy);
        LocationSharingSession session = sessionRepository.findByTokenHashForUpdate(hash(token))
                .orElseThrow(this::unavailable);
        expireIfNeeded(session);
        if (session.getStatus() != LocationSharingStatus.PENDING) throw unavailable();
        session.setLatitude(latitude);
        session.setLongitude(longitude);
        session.setAccuracyMeters(accuracy);
        session.setStatus(LocationSharingStatus.COMPLETED);
        session.setConsumedAt(LocalDateTime.now());
    }

    @Override
    @Transactional
    public LocationSharingSessionResponseDto cancel(UUID sessionId, String email) {
        User user = citizen(email);
        LocationSharingSession session = sessionRepository.findOwnedForUpdate(sessionId, user.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Location sharing session not found"));
        expireIfNeeded(session);
        if (session.getStatus() == LocationSharingStatus.PENDING) session.setStatus(LocationSharingStatus.CANCELLED);
        return response(session);
    }

    private void expireIfNeeded(LocationSharingSession session) {
        if (session.getStatus() == LocationSharingStatus.PENDING && !session.getExpiresAt().isAfter(LocalDateTime.now())) session.setStatus(LocationSharingStatus.EXPIRED);
    }

    private void validateLocation(BigDecimal latitude, BigDecimal longitude, BigDecimal accuracy) {
        if (latitude == null || longitude == null || accuracy == null
                || latitude.compareTo(BigDecimal.valueOf(-90)) < 0 || latitude.compareTo(BigDecimal.valueOf(90)) > 0
                || longitude.compareTo(BigDecimal.valueOf(-180)) < 0 || longitude.compareTo(BigDecimal.valueOf(180)) > 0
                || accuracy.signum() < 0 || accuracy.compareTo(maxAccuracy) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Location is missing or does not meet the required accuracy");
        }
    }

    private User citizen(String email) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User account not found"));
        if (user.getUserRole() != Role.ROLE_CITIZEN) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only citizens can use location sharing");
        return user;
    }

    private LocationSharingSession ownerSession(UUID sessionId, User user) {
        return sessionRepository.findBySessionIdAndUser_UserId(sessionId, user.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Location sharing session not found"));
    }

    private LocationSharingSessionResponseDto response(LocationSharingSession session) {
        boolean complete = session.getStatus() == LocationSharingStatus.COMPLETED;
        return new LocationSharingSessionResponseDto(session.getSessionId(), session.getStatus(), session.getExpiresAt(),
                complete ? session.getLatitude() : null, complete ? session.getLongitude() : null,
                complete ? session.getAccuracyMeters() : null);
    }

    private String hash(String token) {
        if (token == null || token.isBlank() || token.length() > 256) throw unavailable();
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private ResponseStatusException unavailable() {
        return new ResponseStatusException(HttpStatus.GONE, "This location sharing link is invalid or no longer available");
    }
}
