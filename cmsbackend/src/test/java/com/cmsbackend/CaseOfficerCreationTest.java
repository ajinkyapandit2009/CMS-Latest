package com.cmsbackend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import com.cmsbackend.entities.CaseOfficer;
import com.cmsbackend.entities.User;
import com.cmsbackend.enums.Role;
import com.cmsbackend.repositories.CaseOfficerRepository;
import com.cmsbackend.repositories.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@AutoConfigureMockMvc
@SpringBootTest(properties = {
    "server.ssl.enabled=false", "server.ssl.certificate=", "server.ssl.certificate-private-key=",
    "spring.datasource.url=jdbc:h2:mem:cms_officer_atomic;MODE=MySQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa",
    "spring.datasource.password=", "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
    "spring.jpa.show-sql=false", "spring.app.jwtSecret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
})
class CaseOfficerCreationTest {
    @Autowired private MockMvc mvc;
    @Autowired private UserRepository users;
    @Autowired private CaseOfficerRepository officers;
    @Autowired private PasswordEncoder passwords;
    private final ObjectMapper json = new ObjectMapper();

    private User user(Role role, String suffix) {
        User user = new User();
        user.setFirstName("Safe");
        user.setLastName("Fixture");
        user.setEmail("officer-atomic-" + suffix + "@example.test");
        user.setPassword(passwords.encode("StrongPass9"));
        user.setUserRole(role);
        return users.saveAndFlush(user);
    }

    private String token(Role role, String suffix) throws Exception {
        User user = user(role, suffix);
        String response = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("email", user.getEmail(), "password", "StrongPass9"))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(response).get("token").asText();
    }

    private String request(String suffix) throws Exception {
        Map<String, String> values = new HashMap<>();
        values.put("firstName", "New");
        values.put("lastName", "Officer");
        values.put("email", "Atomic-" + suffix + "@Example.Test");
        values.put("password", "StrongPass9");
        values.put("confirmPassword", "StrongPass9");
        return json.writeValueAsString(values);
    }

    private JsonNode create(String token, String suffix) throws Exception {
        String response = mvc.perform(post("/api/admin/case-officers/with-account")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(request(suffix)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return json.readTree(response);
    }

    @Test
    void legacyTwoStepRoutesAreGoneWithoutCreatingPartialAccounts() throws Exception {
        String admin = token(Role.ROLE_ADMIN, "legacy-admin");
        String citizen = token(Role.ROLE_CITIZEN, "legacy-citizen");
        long usersBefore = users.count();
        long officersBefore = officers.count();
        mvc.perform(post("/api/admin/case-officer-accounts")
                .contentType(MediaType.APPLICATION_JSON).content(request("legacy-account")))
                .andExpect(status().isUnauthorized());
        for (String route : new String[] {
                "/api/admin/case-officer-accounts", "/api/admin/case-officers" }) {
            mvc.perform(post(route).header("Authorization", "Bearer " + citizen)
                    .contentType(MediaType.APPLICATION_JSON).content(request("legacy-citizen")))
                    .andExpect(status().isForbidden());
            mvc.perform(post(route).header("Authorization", "Bearer " + admin)
                    .contentType(MediaType.APPLICATION_JSON).content(request("legacy-admin")))
                    .andExpect(status().isGone());
        }
        assertEquals(usersBefore, users.count());
        assertEquals(officersBefore, officers.count());
        mvc.perform(get("/api/admin/case-officers").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk());
        mvc.perform(get("/api/admin/case-officers/workloads").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk());
    }

    @Test
    void createsAccountAndProfileTogetherWithAdminAuthorization() throws Exception {
        String admin = token(Role.ROLE_ADMIN, "auth-admin");
        String citizen = token(Role.ROLE_CITIZEN, "auth-citizen");
        mvc.perform(post("/api/admin/case-officers/with-account")
                .contentType(MediaType.APPLICATION_JSON).content(request("anonymous")))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/admin/case-officers/with-account")
                .header("Authorization", "Bearer " + citizen)
                .contentType(MediaType.APPLICATION_JSON).content(request("citizen")))
                .andExpect(status().isForbidden());
        JsonNode created = create(admin, "first");
        assertTrue(created.get("employeeNumber").asText().matches("CMS-OFF-[0-9]{3,}"));
        assertEquals("atomic-first@example.test", created.get("email").asText());
        int userId = created.get("userId").asInt();
        User account = users.findById(userId).orElseThrow();
        assertEquals(Role.ROLE_OFFICER, account.getUserRole());
        assertTrue(passwords.matches("StrongPass9", account.getPassword()));
        assertEquals(created.get("employeeNumber").asText(), officers.findById(userId).orElseThrow().getEmployeeNumber());
        mvc.perform(post("/api/admin/case-officers/with-account")
                .header("Authorization", "Bearer " + admin)
                .contentType(MediaType.APPLICATION_JSON).content(request("first")))
                .andExpect(status().isConflict());
    }

    @Test
    void requestLoggingDoesNotIncludeSubmittedPasswords() {
        com.cmsbackend.dtos.UserRequestDto request = new com.cmsbackend.dtos.UserRequestDto();
        request.setPassword("SensitivePass9");
        request.setConfirmPassword("SensitivePass9");
        assertTrue(!request.toString().contains("SensitivePass9"));
    }

    @Test
    void concurrentCreationUsesUniqueNumericSuffixBeyond999() throws Exception {
        String admin = token(Role.ROLE_ADMIN, "concurrency-admin");
        for (String number : new String[] { "CMS-OFF-999", "CMS-OFF-1000" }) {
            User existing = user(Role.ROLE_OFFICER, "existing-" + number);
            CaseOfficer profile = new CaseOfficer();
            profile.setUser(existing);
            profile.setEmployeeNumber(number);
            officers.saveAndFlush(profile);
        }
        var pool = Executors.newFixedThreadPool(2);
        var start = new CountDownLatch(1);
        try {
            var first = pool.submit(() -> { start.await(); return create(admin, "concurrent-one"); });
            var second = pool.submit(() -> { start.await(); return create(admin, "concurrent-two"); });
            start.countDown();
            JsonNode one = first.get(20, TimeUnit.SECONDS);
            JsonNode two = second.get(20, TimeUnit.SECONDS);
            assertNotEquals(one.get("employeeNumber").asText(), two.get("employeeNumber").asText());
            assertTrue(java.util.Set.of(one.get("employeeNumber").asText(), two.get("employeeNumber").asText())
                    .containsAll(java.util.Set.of("CMS-OFF-1001", "CMS-OFF-1002")));
            assertTrue(officers.existsById(one.get("userId").asInt()));
            assertTrue(officers.existsById(two.get("userId").asInt()));
        } finally {
            pool.shutdownNow();
        }
    }
}
