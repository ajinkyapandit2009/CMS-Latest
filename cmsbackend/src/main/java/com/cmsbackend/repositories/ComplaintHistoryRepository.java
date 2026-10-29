
package com.cmsbackend.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.cmsbackend.entities.ComplaintHistory;

@Repository
public interface ComplaintHistoryRepository
        extends JpaRepository<ComplaintHistory, Integer> {

}
