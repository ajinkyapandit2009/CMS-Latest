
package com.cmsbackend.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.cmsbackend.entities.Citizen;

@Repository
public interface CitizenRepository extends JpaRepository<Citizen, Integer> {

}
