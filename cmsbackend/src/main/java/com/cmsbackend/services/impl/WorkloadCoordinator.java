package com.cmsbackend.services.impl;

import org.springframework.stereotype.Component;
import com.cmsbackend.repositories.UserRepository;

@Component
public class WorkloadCoordinator {
    private final UserRepository userRepository;

    public WorkloadCoordinator(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // Call inside the caller's write transaction before reading or changing workload.
    // The first user always exists for authenticated complaint and officer operations.
    public void lock() {
        userRepository.findFirstByOrderByUserIdAsc()
                .orElseThrow(() -> new IllegalStateException("No user row available for workload coordination"));
    }
}
