
package com.cmsbackend.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

import com.cmsbackend.entities.CaseOfficer;
import com.cmsbackend.enums.ComplaintStatus;

@Repository
public interface CaseOfficerRepository
        extends JpaRepository<CaseOfficer, Integer> {

	
	boolean existsByEmployeeNumber(String employeeNumber);
	
	CaseOfficer findTopByOrderByEmployeeNumberDesc();

	@Query("SELECT o.employeeNumber FROM CaseOfficer o")
	List<String> findAllEmployeeNumbers();

	List<CaseOfficer> findAllByOrderByEmployeeNumberAsc();

	@Query("""
		SELECT o.userId AS userId, u.firstName AS firstName, u.lastName AS lastName,
		       o.employeeNumber AS employeeNumber,
		       COUNT(CASE WHEN c.status = :assigned THEN c.complaintId ELSE NULL END) AS assignedCount,
		       COUNT(CASE WHEN c.status = :inProgress THEN c.complaintId ELSE NULL END) AS inProgressCount
		FROM CaseOfficer o JOIN o.user u LEFT JOIN o.complaint c
		GROUP BY o.userId, u.firstName, u.lastName, o.employeeNumber
		ORDER BY o.employeeNumber ASC
		""")
	List<OfficerWorkloadCount> findWorkloadCounts(
		@Param("assigned") ComplaintStatus assigned,
		@Param("inProgress") ComplaintStatus inProgress);
}
