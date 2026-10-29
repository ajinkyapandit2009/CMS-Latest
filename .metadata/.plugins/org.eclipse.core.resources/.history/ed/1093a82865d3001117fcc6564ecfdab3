
package com.cmsbackend.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.cmsbackend.entities.CaseOfficer;

@Repository
public interface CaseOfficerRepository
        extends JpaRepository<CaseOfficer, Integer> {

	
	boolean existsByEmployeeNumber(String employeeNumber);
}
