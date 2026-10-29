package com.cmsbackend.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.cmsbackend.entities.Complaint;

@Repository
public interface ComplaintRepository
        extends JpaRepository<Complaint, Integer> {

    List<Complaint> findByCitizen_UserIdOrderByCreatedAtDesc(
            Integer citizenId
    );
    
    List<Complaint> findByOfficer_UserIdOrderByCreatedAtDesc(
            Integer officerId
    );
}