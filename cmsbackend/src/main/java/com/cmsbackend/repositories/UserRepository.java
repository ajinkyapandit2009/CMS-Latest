
package com.cmsbackend.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;

import com.cmsbackend.entities.User;
import com.cmsbackend.enums.Role;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    Optional<User> findByEmail(String email);
    boolean existsByEmailIgnoreCase(String email);
    java.util.List<User> findByUserRole(Role userRole);

    // A stable row shared by all workload-changing transactions, including when no officers exist.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<User> findFirstByOrderByUserIdAsc();
}
