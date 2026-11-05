package com.cmsbackend.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.repository.core.support.AbstractRepositoryMetadata;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.core.mapping.RepositoryDetectionStrategy;
import org.springframework.data.rest.core.mapping.RepositoryDetectionStrategy.RepositoryDetectionStrategies;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import com.cmsbackend.repositories.*;

class RepositoryRestSecurityConfigTest {
    @Test
    void sensitiveRepositoriesCannotBeExportedEvenThoughTheFrameworkDefaultExportsThem() {
        RepositoryRestConfiguration configuration = mock(RepositoryRestConfiguration.class);
        new RepositoryRestSecurityConfig().configureRepositoryRestConfiguration(configuration, new CorsRegistry());
        ArgumentCaptor<RepositoryDetectionStrategy> strategy = ArgumentCaptor.forClass(RepositoryDetectionStrategy.class);
        verify(configuration).setRepositoryDetectionStrategy(strategy.capture());

        for (Class<?> repository : List.of(UserRepository.class, CitizenRepository.class, CaseOfficerRepository.class,
                ComplaintRepository.class, ComplaintHistoryRepository.class, NotificationRepository.class,
                ComplaintPhotoUploadRepository.class, LocationSharingSessionRepository.class)) {
            var metadata = AbstractRepositoryMetadata.getMetadata(repository);
            assertTrue(RepositoryDetectionStrategies.DEFAULT.isExported(metadata), repository.getSimpleName());
            assertFalse(strategy.getValue().isExported(metadata), repository.getSimpleName());
        }
    }
}
