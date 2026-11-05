package com.cmsbackend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

import com.cmsbackend.entities.User;
import com.cmsbackend.enums.Role;
import com.cmsbackend.repositories.CitizenRepository;
import com.cmsbackend.repositories.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@AutoConfigureMockMvc
@SpringBootTest(properties = {
    "server.ssl.enabled=false", "server.ssl.certificate=", "server.ssl.certificate-private-key=",
    "spring.datasource.url=jdbc:h2:mem:cms_registration;MODE=MySQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa",
    "spring.datasource.password=", "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
    "spring.jpa.show-sql=false",
    "logging.level.root=WARN", "logging.level.org.springframework.web=INFO", "logging.level.org.hibernate.SQL=ERROR",
    "spring.app.jwtSecret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
})
class RegistrationValidationTest {
    @Autowired private MockMvc mvc;
    @Autowired private UserRepository users;
    @Autowired private CitizenRepository citizens;
    @Autowired private PasswordEncoder passwords;
    private final ObjectMapper json = new ObjectMapper();

    private Map<String, String> valid(String suffix) {
        Map<String, String> data = new HashMap<>();
        data.put("firstName", " Safe ");
        data.put("lastName", " Citizen ");
        data.put("email", "reg-" + suffix + "@example.test");
        data.put("password", "StrongPass9");
        data.put("confirmPassword", "StrongPass9");
        data.put("phone", " +919876543210 ");
        data.put("address", "  Test address  ");
        return data;
    }

    private JsonNode register(Map<String, String> data, int expectedStatus) throws Exception {
        String response = mvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(data)))
                .andExpect(status().is(expectedStatus)).andReturn().getResponse().getContentAsString();
        return json.readTree(response);
    }

    @Test
    void publicRegistrationNormalizesAndCreatesOnlyBCryptCitizen() throws Exception {
        Map<String, String> data = valid("normalized");
        data.put("email", "  REG-Normalized@Example.Test  ");
        JsonNode created = register(data, 201);
        assertEquals("reg-normalized@example.test", created.get("email").asText());
        assertEquals("ROLE_CITIZEN", created.get("role").asText());
        assertEquals("Safe", created.get("firstName").asText());
        assertFalse(created.has("password"));
        User saved = users.findByEmail("reg-normalized@example.test").orElseThrow();
        assertEquals(Role.ROLE_CITIZEN, saved.getUserRole());
        assertTrue(passwords.matches("StrongPass9", saved.getPassword()));
        assertTrue(citizens.existsById(saved.getUserId()));
        assertEquals("+919876543210", saved.getPhone());
        assertEquals("Test address", saved.getAddress());
        JsonNode duplicate = register(valid("normalized"), 409);
        assertEquals("Email is already registered", duplicate.get("fieldErrors").get("email").asText());
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"REG-NORMALIZED@example.test\",\"password\":\"StrongPass9\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void fieldErrorsAndRoleClaimsAreRejectedWithoutAccountCreation() throws Exception {
        for (String field : new String[] { "role", "userRole" }) {
            Map<String, String> data = valid("role-" + field);
            data.put(field, "ROLE_ADMIN");
            assertTrue(register(data, 400).get("fieldErrors").has("role"));
            assertFalse(users.existsByEmailIgnoreCase(data.get("email")));
        }
        Map<String, String> invalid = valid("invalid");
        invalid.put("firstName", "   ");
        invalid.put("lastName", " ");
        invalid.put("email", "invalid-address");
        invalid.put("password", "weak");
        invalid.put("phone", "letters");
        JsonNode fields = register(invalid, 400).get("fieldErrors");
        for (String field : new String[] { "firstName", "lastName", "email", "password", "phone" }) {
            assertTrue(fields.has(field), "Missing error for " + field);
        }
        Map<String, String> invalidName = valid("invalid-name");
        invalidName.put("firstName", "1234");
        assertTrue(register(invalidName, 400).get("fieldErrors").has("firstName"));
        Map<String, String> mismatch = valid("mismatch");
        mismatch.put("confirmPassword", "DifferentPass1");
        assertTrue(register(mismatch, 400).get("fieldErrors").has("confirmPassword"));
        Map<String, String> overlong = valid("overlong");
        overlong.put("address", "😀".repeat(17000));
        assertTrue(register(overlong, 400).get("fieldErrors").has("address"));
        Map<String, String> longPassword = valid("long-password");
        longPassword.put("password", "StrongPass9" + "😀".repeat(20));
        longPassword.put("confirmPassword", longPassword.get("password"));
        assertTrue(register(longPassword, 400).get("fieldErrors").has("password"));
    }

    @Test
    void availabilityIsPublicNormalizedAdvisoryAndRateLimited() throws Exception {
        register(valid("availability"), 201);
        String path = "/users/email-availability";
        JsonNode taken = json.readTree(mvc.perform(get(path).param("email", " REG-AVAILABILITY@EXAMPLE.TEST ")
                .with(request -> { request.setRemoteAddr("192.0.2.4"); return request; }))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        assertFalse(taken.get("available").asBoolean());
        assertEquals(1, taken.size());
        JsonNode free = json.readTree(mvc.perform(get(path).param("email", "new-address@example.test")
                .with(request -> { request.setRemoteAddr("192.0.2.4"); return request; }))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        assertTrue(free.get("available").asBoolean());
        String invalid = mvc.perform(get(path).param("email", "bad-address")
                .with(request -> { request.setRemoteAddr("192.0.2.5"); return request; }))
                .andExpect(status().isBadRequest()).andReturn().getResponse().getContentAsString();
        assertTrue(json.readTree(invalid).get("fieldErrors").has("email"));
        for (int i = 0; i < 8; i++) {
            mvc.perform(get(path).param("email", "free@example.test")
                    .with(request -> { request.setRemoteAddr("192.0.2.4"); return request; }))
                    .andExpect(status().isOk());
        }
        mvc.perform(get(path).param("email", "free@example.test")
                .with(request -> { request.setRemoteAddr("192.0.2.4"); return request; }))
                .andExpect(status().isTooManyRequests());
        mvc.perform(get(path).param("email", "free@example.test")
                .with(request -> { request.setRemoteAddr("192.0.2.6"); return request; }))
                .andExpect(status().isOk());
    }

    @Test
    void concurrentRegistrationsReturnOneCreatedAndOneEmailConflict() throws Exception {
        Map<String, String> data = valid("concurrent");
        var pool = Executors.newFixedThreadPool(2);
        var start = new CountDownLatch(1);
        try {
            var action = (java.util.concurrent.Callable<Integer>) () -> {
                start.await();
                return mvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(data))).andReturn().getResponse().getStatus();
            };
            var first = pool.submit(action);
            var second = pool.submit(action);
            start.countDown();
            int firstStatus = first.get(15, TimeUnit.SECONDS);
            int secondStatus = second.get(15, TimeUnit.SECONDS);
            assertEquals(1, (firstStatus == 201 ? 1 : 0) + (secondStatus == 201 ? 1 : 0));
            assertEquals(1, (firstStatus == 409 ? 1 : 0) + (secondStatus == 409 ? 1 : 0));
        } finally {
            pool.shutdownNow();
        }
        assertEquals(1, users.findAll().stream().filter(user -> data.get("email").equals(user.getEmail())).count());
    }

    @Test
    void adminOfficerCreationUsesAtomicEndpoint() throws Exception {
        User admin = new User();
        admin.setFirstName("Admin");
        admin.setLastName("Fixture");
        admin.setEmail("registration-admin@example.test");
        admin.setPassword(passwords.encode("StrongPass9"));
        admin.setUserRole(Role.ROLE_ADMIN);
        users.saveAndFlush(admin);
        String login = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"registration-admin@example.test\",\"password\":\"StrongPass9\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String token = json.readTree(login).get("token").asText();
        String created = mvc.perform(post("/api/admin/case-officers/with-account")
                .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(valid("officer"))))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        assertEquals("reg-officer@example.test", json.readTree(created).get("email").asText());
        assertEquals(Role.ROLE_OFFICER, users.findByEmail("reg-officer@example.test").orElseThrow().getUserRole());
    }
}
