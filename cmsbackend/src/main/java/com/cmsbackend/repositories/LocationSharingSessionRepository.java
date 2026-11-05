package com.cmsbackend.repositories;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.cmsbackend.entities.LocationSharingSession;
import jakarta.persistence.LockModeType;

public interface LocationSharingSessionRepository extends JpaRepository<LocationSharingSession, UUID> {
    Optional<LocationSharingSession> findBySessionIdAndUser_UserId(UUID sessionId, Integer userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select session from LocationSharingSession session where session.tokenHash = :tokenHash")
    Optional<LocationSharingSession> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select session from LocationSharingSession session where session.sessionId = :sessionId and session.user.userId = :userId")
    Optional<LocationSharingSession> findOwnedForUpdate(@Param("sessionId") UUID sessionId, @Param("userId") Integer userId);
}
