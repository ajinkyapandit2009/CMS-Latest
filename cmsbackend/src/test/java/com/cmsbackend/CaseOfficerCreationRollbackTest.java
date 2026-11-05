package com.cmsbackend;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.cmsbackend.dtos.CaseOfficerRequestDto;
import com.cmsbackend.dtos.UserRequestDto;
import com.cmsbackend.entities.User;
import com.cmsbackend.enums.Role;
import com.cmsbackend.repositories.UserRepository;
import com.cmsbackend.services.AdminService;
import com.cmsbackend.services.CaseOfficerService;

@SpringBootTest(properties = {
    "server.ssl.enabled=false", "server.ssl.certificate=", "server.ssl.certificate-private-key=",
    "spring.datasource.url=jdbc:h2:mem:cms_officer_rollback;MODE=MySQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa",
    "spring.datasource.password=", "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
    "spring.jpa.show-sql=false", "spring.app.jwtSecret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
})
class CaseOfficerCreationRollbackTest {
    @Autowired private AdminService adminService;
    @Autowired private UserRepository users;
    @Autowired private PasswordEncoder passwords;
    @MockitoBean private CaseOfficerService caseOfficerService;

    @Test
    void profileFailureRollsBackNewUserAccount() {
        User admin = new User();
        admin.setFirstName("Admin");
        admin.setLastName("Fixture");
        admin.setEmail("officer-rollback-admin@example.test");
        admin.setPassword(passwords.encode("StrongPass9"));
        admin.setUserRole(Role.ROLE_ADMIN);
        users.saveAndFlush(admin);
        when(caseOfficerService.createCaseOfficer(any(CaseOfficerRequestDto.class)))
                .thenThrow(new IllegalStateException("Synthetic profile failure"));
        UserRequestDto request = new UserRequestDto();
        request.setFirstName("New");
        request.setLastName("Officer");
        request.setEmail("officer-rollback-new@example.test");
        request.setPassword("StrongPass9");
        request.setConfirmPassword("StrongPass9");
        assertThrows(IllegalStateException.class, () -> adminService.createCaseOfficerWithProfile(request));
        assertFalse(users.existsByEmailIgnoreCase("officer-rollback-new@example.test"));
    }
}
