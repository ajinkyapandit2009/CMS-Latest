package com.cmsbackend.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.cmsbackend.entities.Complaint;
import com.cmsbackend.enums.ComplaintPriority;
import com.cmsbackend.enums.ComplaintStatus;

@Repository
public interface ComplaintRepository
        extends JpaRepository<Complaint, Integer> {

    List<Complaint> findByCitizen_UserIdOrderByCreatedAtDesc(
            Integer citizenId
    );
    
    List<Complaint> findByOfficer_UserIdOrderByCreatedAtDesc(
            Integer officerId
    );
    
    @Query("""
    	    SELECT c
    	    FROM Complaint c
    	    WHERE (:status IS NULL OR c.status = :status)
    	      AND (:priority IS NULL OR c.priority = :priority)
    	    ORDER BY c.createdAt DESC
    	    """)
    	List<Complaint> findWithOptionalFilters(
    	        @Param("status") ComplaintStatus status,
    	        @Param("priority") ComplaintPriority priority
    	);
}