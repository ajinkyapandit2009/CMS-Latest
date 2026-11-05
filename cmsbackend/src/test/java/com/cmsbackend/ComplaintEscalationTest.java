package com.cmsbackend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import com.cmsbackend.entities.Citizen;
import com.cmsbackend.entities.Complaint;
import com.cmsbackend.entities.User;
import com.cmsbackend.enums.ComplaintStatus;
import com.cmsbackend.enums.Role;
import com.cmsbackend.repositories.CitizenRepository;
import com.cmsbackend.repositories.ComplaintRepository;
import com.cmsbackend.repositories.NotificationRepository;
import com.cmsbackend.repositories.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@AutoConfigureMockMvc
@SpringBootTest(properties = {
    "server.ssl.enabled=false", "server.ssl.certificate=", "server.ssl.certificate-private-key=",
    "spring.datasource.url=jdbc:h2:mem:cms_escalation;MODE=MySQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa",
    "spring.datasource.password=", "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
    "spring.jpa.show-sql=false",
    "spring.app.jwtSecret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
})
class ComplaintEscalationTest {
    @Autowired private MockMvc mvc;
    @Autowired private UserRepository users;
    @Autowired private CitizenRepository citizens;
    @Autowired private ComplaintRepository complaints;
    @Autowired private NotificationRepository notifications;
    @Autowired private PasswordEncoder passwords;
    @Autowired private JdbcTemplate jdbc;
    private final ObjectMapper json = new ObjectMapper();

    private record Fixture(User user, String token) {}

    private Fixture user(Role role, String suffix) throws Exception {
        User user = new User();
        user.setFirstName("Test");
        user.setLastName(suffix);
        user.setEmail("escalation-" + suffix + "@example.test");
        user.setPassword(passwords.encode("fixture-password"));
        user.setUserRole(role);
        user = users.saveAndFlush(user);
        if (role == Role.ROLE_CITIZEN) {
            Citizen citizen = new Citizen();
            citizen.setUser(user);
            citizens.saveAndFlush(citizen);
        }
        String response = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + user.getEmail() + "\",\"password\":\"fixture-password\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return new Fixture(user, json.readTree(response).get("token").asText());
    }

    private Complaint complaint(Fixture owner, ComplaintStatus status) {
        Complaint complaint = new Complaint();
        complaint.setCitizen(citizens.findById(owner.user().getUserId()).orElseThrow());
        complaint.setTitle("Escalation fixture");
        complaint.setDescription("Safe test record");
        complaint.setCategory("Roads");
        complaint.setAddress("Test address");
        complaint.setStatus(status);
        return complaints.saveAndFlush(complaint);
    }

    private String auth(Fixture fixture) {
        return "Bearer " + fixture.token();
    }

    @Test
    void enforcesOwnershipRoleReasonAndFortyEightHours() throws Exception {
        Fixture owner = user(Role.ROLE_CITIZEN, "elig-owner");
        Fixture other = user(Role.ROLE_CITIZEN, "elig-other");
        Fixture officer = user(Role.ROLE_OFFICER, "elig-officer");
        Complaint complaint = complaint(owner, ComplaintStatus.SUBMITTED);
        String path = "/api/escalations/complaints/" + complaint.getComplaintId();
        mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"Help\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post(path).header("Authorization", auth(officer)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"Help\"}")).andExpect(status().isForbidden());
        mvc.perform(post(path).header("Authorization", auth(other)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"Help\"}")).andExpect(status().isNotFound());
        mvc.perform(post(path).header("Authorization", auth(owner)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"   \"}")).andExpect(status().isBadRequest());
        mvc.perform(post(path).header("Authorization", auth(owner)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"Help\"}")).andExpect(status().isConflict());
        jdbc.update("UPDATE complaint SET created_at = ? WHERE complaint_id = ?",
                Timestamp.valueOf(LocalDateTime.now().minusHours(49)), complaint.getComplaintId());
        mvc.perform(post(path).header("Authorization", auth(owner)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"  Help  \"}")).andExpect(status().isCreated());
    }

    @Test
    void reviewAuditsAndNotifiesWithoutChangingComplaintStatus() throws Exception {
        Fixture owner = user(Role.ROLE_CITIZEN, "review-owner");
        Fixture other = user(Role.ROLE_CITIZEN, "review-other");
        Fixture admin = user(Role.ROLE_ADMIN, "review-admin");
        Complaint complaint = complaint(owner, ComplaintStatus.REJECTED);
        String submitPath = "/api/escalations/complaints/" + complaint.getComplaintId();
        JsonNode submitted = json.readTree(mvc.perform(post(submitPath)
                .header("Authorization", auth(owner)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"Please reconsider\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
        int id = submitted.get("escalationId").asInt();
        assertEquals("PENDING", submitted.get("reviewStatus").asText());
        assertEquals(1, notifications.countByUser_UserIdAndIsReadFalse(admin.user().getUserId()));
        mvc.perform(post(submitPath).header("Authorization", auth(owner)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"Again\"}")).andExpect(status().isConflict());
        mvc.perform(get("/api/escalations/" + id).header("Authorization", auth(other)))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/escalations/my").header("Authorization", auth(other)))
                .andExpect(status().isOk());
        mvc.perform(get("/api/admin/escalations").header("Authorization", auth(owner)))
                .andExpect(status().isForbidden());
        mvc.perform(patch("/api/admin/escalations/" + id + "/review")
                .header("Authorization", auth(admin)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"PENDING\",\"remarks\":\"No\"}"))
                .andExpect(status().isBadRequest());
        JsonNode reviewed = json.readTree(mvc.perform(patch("/api/admin/escalations/" + id + "/review")
                .header("Authorization", auth(admin)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"ACCEPTED\",\"remarks\":\"  Reviewed  \"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        assertEquals("ACCEPTED", reviewed.get("reviewStatus").asText());
        assertEquals("Reviewed", reviewed.get("remarks").asText());
        assertEquals(admin.user().getUserId(), reviewed.get("reviewedById").asInt());
        assertNotNull(reviewed.get("reviewedAt"));
        assertEquals(ComplaintStatus.REJECTED, complaints.findById(complaint.getComplaintId()).orElseThrow().getStatus());
        assertEquals(1, notifications.countByUser_UserIdAndIsReadFalse(owner.user().getUserId()));
        mvc.perform(patch("/api/admin/escalations/" + id + "/review")
                .header("Authorization", auth(admin)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"DECLINED\",\"remarks\":\"Again\"}"))
                .andExpect(status().isConflict());
        mvc.perform(post(submitPath).header("Authorization", auth(owner)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"New review\"}")).andExpect(status().isCreated());
        String pending = mvc.perform(get("/api/admin/escalations?status=PENDING")
                .header("Authorization", auth(admin))).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertEquals(1, json.readTree(pending).size());
        int secondId = json.readTree(pending).get(0).get("escalationId").asInt();
        mvc.perform(patch("/api/admin/escalations/" + secondId + "/review")
                .header("Authorization", auth(admin)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"DECLINED\",\"remarks\":\"Insufficient evidence\"}"))
                .andExpect(status().isOk());
        assertEquals(2, notifications.countByUser_UserIdAndIsReadFalse(owner.user().getUserId()));
    }

    @Test
    void simultaneousSubmissionsLeaveOnlyOnePendingEscalation() throws Exception {
        Fixture owner = user(Role.ROLE_CITIZEN, "concurrent-owner");
        Complaint complaint = complaint(owner, ComplaintStatus.RESOLVED);
        String path = "/api/escalations/complaints/" + complaint.getComplaintId();
        var pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            var request = (java.util.concurrent.Callable<Integer>) () -> {
                start.await();
                return mvc.perform(post(path).header("Authorization", auth(owner))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"Review\"}"))
                        .andReturn().getResponse().getStatus();
            };
            var first = pool.submit(request);
            var second = pool.submit(request);
            start.countDown();
            int firstStatus = first.get(15, TimeUnit.SECONDS);
            int secondStatus = second.get(15, TimeUnit.SECONDS);
            assertEquals(1, (firstStatus == 201 ? 1 : 0) + (secondStatus == 201 ? 1 : 0));
            assertEquals(1, (firstStatus == 409 ? 1 : 0) + (secondStatus == 409 ? 1 : 0));
        } finally {
            pool.shutdownNow();
        }
        String own = mvc.perform(get("/api/escalations/my").header("Authorization", auth(owner)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertEquals(1, json.readTree(own).size());
    }
}
