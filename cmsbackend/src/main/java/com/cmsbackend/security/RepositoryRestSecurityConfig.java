package com.cmsbackend.security;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurer;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

/** Keep JPA repositories internal; HTTP access goes through authorized controllers. */
@Configuration
public class RepositoryRestSecurityConfig implements RepositoryRestConfigurer {
    @Override
    public void configureRepositoryRestConfiguration(RepositoryRestConfiguration configuration, CorsRegistry cors) {
        configuration.setRepositoryDetectionStrategy(metadata -> false);
        configuration.setExposeRepositoryMethodsByDefault(false);
    }
}
