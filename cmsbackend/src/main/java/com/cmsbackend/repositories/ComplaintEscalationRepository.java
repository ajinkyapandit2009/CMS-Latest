package com.cmsbackend.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.cmsbackend.entities.ComplaintEscalation;
import com.cmsbackend.enums.EscalationReviewStatus;

import jakarta.persistence.LockModeType;

public interface ComplaintEscalationRepository extends JpaRepository<ComplaintEscalation, Integer> {
    boolean existsByComplaint_ComplaintIdAndReviewStatus(Integer complaintId, EscalationReviewStatus status);

    List<ComplaintEscalation> findByCitizen_UserIdOrderByCreatedAtDesc(Integer citizenId);

    Optional<ComplaintEscalation> findByEscalationIdAndCitizen_UserId(Integer escalationId, Integer citizenId);

    List<ComplaintEscalation> findAllByOrderByCreatedAtDesc();

    List<ComplaintEscalation> findByReviewStatusOrderByCreatedAtDesc(EscalationReviewStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM ComplaintEscalation e WHERE e.escalationId = :escalationId")
    Optional<ComplaintEscalation> findByIdForReview(@Param("escalationId") Integer escalationId);
}
