package com.cmsbackend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cmsbackend.entities.CaseOfficer;
import com.cmsbackend.entities.Citizen;
import com.cmsbackend.entities.Complaint;
import com.cmsbackend.entities.Notification;
import com.cmsbackend.entities.User;
import com.cmsbackend.enums.ComplaintStatus;
import com.cmsbackend.enums.Role;
import com.cmsbackend.repositories.CaseOfficerRepository;
import com.cmsbackend.repositories.CitizenRepository;
import com.cmsbackend.repositories.ComplaintRepository;
import com.cmsbackend.repositories.ComplaintHistoryRepository;
import com.cmsbackend.repositories.NotificationRepository;
import com.cmsbackend.repositories.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@AutoConfigureMockMvc
@SpringBootTest(properties = {
    "server.ssl.enabled=false",
    "server.ssl.certificate=",
    "server.ssl.certificate-private-key=",
    "spring.datasource.url=jdbc:h2:mem:cms_regression;MODE=MySQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
    "spring.jpa.show-sql=false",
    "spring.app.jwtSecret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
})
class CmsbackendApplicationTests {
    @Autowired private MockMvc mvc;
    @Autowired private UserRepository users;
    @Autowired private CitizenRepository citizens;
    @Autowired private CaseOfficerRepository officers;
    @Autowired private ComplaintRepository complaints;
    @Autowired private ComplaintHistoryRepository histories;
    @Autowired private NotificationRepository notifications;
    @Autowired private PasswordEncoder passwords;
    private final ObjectMapper json = new ObjectMapper();

    @Test
    void contextLoadsAgainstIsolatedInMemoryDatabase() {
    }

    private String tokenFor(Role role) throws Exception {
        return tokenFor(role, "primary");
    }

    private String email(Role role, String suffix) {
        return "regression-" + role.name().toLowerCase() + "-" + suffix + "@example.test";
    }

    private String tokenFor(Role role, String suffix) throws Exception {
        String email = email(role, suffix);
        User user = new User();
        user.setFirstName("Safe");
        user.setLastName("Fixture");
        user.setEmail(email);
        user.setPassword(passwords.encode("fixture-password"));
        user.setUserRole(role);
        users.save(user);
        String body = "{\"email\":\"" + email + "\",\"password\":\"fixture-password\"}";
        String response = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(response).get("token").asText();
    }

    @Test
    @Transactional
    void realRoleTokensEnforceApiBoundaries() throws Exception {
        String citizen = tokenFor(Role.ROLE_CITIZEN);
        String officer = tokenFor(Role.ROLE_OFFICER);
        String admin = tokenFor(Role.ROLE_ADMIN);
        mvc.perform(get("/auth/me").header("Authorization", "Bearer " + citizen))
                .andExpect(status().isOk());
        mvc.perform(get("/api/admin/complaints").header("Authorization", "Bearer " + citizen))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/admin/case-officer-accounts").header("Authorization", "Bearer " + citizen)
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/admin/case-officers").header("Authorization", "Bearer " + officer)
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/case-officers").header("Authorization", "Bearer " + officer))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/complaints").header("Authorization", "Bearer " + officer)
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/complaint-photos").header("Authorization", "Bearer " + admin))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/complaints").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk());
    }

    @Test
    @Transactional
    void adminWorkloadAggregatesActiveStatusesAndIncludesIdleOfficers() throws Exception {
        String adminToken = tokenFor(Role.ROLE_ADMIN, "workload-admin");
        String citizenToken = tokenFor(Role.ROLE_CITIZEN, "workload-citizen");
        String officerToken = tokenFor(Role.ROLE_OFFICER, "workload-active");
        tokenFor(Role.ROLE_OFFICER, "workload-idle");

        Citizen citizen = new Citizen();
        citizen.setUser(users.findByEmail(email(Role.ROLE_CITIZEN, "workload-citizen")).orElseThrow());
        citizens.save(citizen);
        CaseOfficer active = new CaseOfficer();
        active.setUser(users.findByEmail(email(Role.ROLE_OFFICER, "workload-active")).orElseThrow());
        active.setEmployeeNumber("CMS-OFF-901");
        officers.save(active);
        CaseOfficer idle = new CaseOfficer();
        idle.setUser(users.findByEmail(email(Role.ROLE_OFFICER, "workload-idle")).orElseThrow());
        idle.setEmployeeNumber("CMS-OFF-902");
        officers.save(idle);

        for (ComplaintStatus status : new ComplaintStatus[] {
                ComplaintStatus.ASSIGNED, ComplaintStatus.IN_PROGRESS,
                ComplaintStatus.RESOLVED, ComplaintStatus.REJECTED, ComplaintStatus.SUBMITTED }) {
            Complaint complaint = new Complaint();
            complaint.setCitizen(citizen);
            complaint.setOfficer(active);
            complaint.setTitle("Workload fixture " + status);
            complaint.setDescription("Isolated test record");
            complaint.setCategory("Roads");
            complaint.setAddress("In-memory address");
            complaint.setStatus(status);
            complaints.save(complaint);
        }
        complaints.flush();

        String path = "/api/admin/case-officers/workloads";
        mvc.perform(get(path)).andExpect(status().isUnauthorized());
        mvc.perform(get(path).header("Authorization", "Bearer " + citizenToken))
                .andExpect(status().isForbidden());
        mvc.perform(get(path).header("Authorization", "Bearer " + officerToken))
                .andExpect(status().isForbidden());
        String response = mvc.perform(get(path).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        var rows = json.readTree(response);
        assertEquals(2, rows.size());
        assertEquals("CMS-OFF-901", rows.get(0).get("employeeNumber").asText());
        assertEquals(1, rows.get(0).get("assignedCount").asInt());
        assertEquals(1, rows.get(0).get("inProgressCount").asInt());
        assertEquals(2, rows.get(0).get("totalActive").asInt());
        assertEquals("CMS-OFF-902", rows.get(1).get("employeeNumber").asText());
        assertEquals(0, rows.get(1).get("assignedCount").asInt());
        assertEquals(0, rows.get(1).get("inProgressCount").asInt());
        assertEquals(0, rows.get(1).get("totalActive").asInt());
    }

    @Test
    @Transactional
    void urgentSubmissionAssignsLeastLoadedOfficerAndNotifiesAllRoles() throws Exception {
        String adminToken = tokenFor(Role.ROLE_ADMIN, "urgent-admin");
        String citizenToken = tokenFor(Role.ROLE_CITIZEN, "urgent-citizen");
        tokenFor(Role.ROLE_OFFICER, "urgent-busy");
        String idleToken = tokenFor(Role.ROLE_OFFICER, "urgent-idle");
        Citizen citizen = new Citizen();
        citizen.setUser(users.findByEmail(email(Role.ROLE_CITIZEN, "urgent-citizen")).orElseThrow());
        citizens.save(citizen);
        CaseOfficer busy = new CaseOfficer();
        busy.setUser(users.findByEmail(email(Role.ROLE_OFFICER, "urgent-busy")).orElseThrow());
        busy.setEmployeeNumber("CMS-OFF-911");
        officers.save(busy);
        CaseOfficer idle = new CaseOfficer();
        idle.setUser(users.findByEmail(email(Role.ROLE_OFFICER, "urgent-idle")).orElseThrow());
        idle.setEmployeeNumber("CMS-OFF-912");
        officers.save(idle);
        Complaint existing = new Complaint();
        existing.setCitizen(citizen);
        existing.setOfficer(busy);
        existing.setTitle("Existing fixture");
        existing.setDescription("Isolated test record");
        existing.setCategory("Roads");
        existing.setAddress("In-memory address");
        existing.setStatus(ComplaintStatus.IN_PROGRESS);
        complaints.saveAndFlush(existing);

        String body = "{\"title\":\"Urgent fixture\",\"description\":\"Safe fixture\","
                + "\"category\":\"Power Cut\",\"address\":\"In-memory address\"}";
        String response = mvc.perform(post("/api/complaints")
                .header("Authorization", "Bearer " + citizenToken)
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        var created = json.readTree(response);
        int complaintId = created.get("complaintId").asInt();
        assertEquals("HIGH", created.get("priority").asText());
        assertEquals("ASSIGNED", created.get("status").asText());
        assertEquals(idle.getUserId(), created.get("officerId").asInt());
        assertEquals(1, histories.findAll().stream()
                .filter(item -> item.getComplaint().getComplaintId().equals(complaintId)
                        && "ASSIGNED".equals(item.getNewStatus())
                        && item.getUpdatedBy() == null)
                .count());
        for (String token : new String[] { citizenToken, idleToken, adminToken }) {
            String inbox = mvc.perform(get("/api/notifications/my")
                    .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
            assertEquals(complaintId, json.readTree(inbox).get(0).get("complaintId").asInt());
        }
    }

    @Test
    @Transactional
    void anonymousRequestsCannotReachProtectedApisAndRepositoriesStayClosed() throws Exception {
        mvc.perform(get("/api/complaints/17")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/location-sharing/sessions/123e4567-e89b-12d3-a456-426614174000"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/location-sharing/fixture-token/location")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"latitude\":18.52,\"longitude\":73.85,\"accuracy\":12}"))
                .andExpect(status().isGone());
        String admin = tokenFor(Role.ROLE_ADMIN);
        mvc.perform(get("/notifications").header("Authorization", "Bearer " + admin))
                .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void complaintHttpReadsRespectOwnershipAndCurrentAssignment() throws Exception {
        String ownerToken = tokenFor(Role.ROLE_CITIZEN, "owner");
        String otherCitizenToken = tokenFor(Role.ROLE_CITIZEN, "other");
        String officerToken = tokenFor(Role.ROLE_OFFICER, "assigned");
        String otherOfficerToken = tokenFor(Role.ROLE_OFFICER, "other");
        String adminToken = tokenFor(Role.ROLE_ADMIN, "reader");

        Citizen owner = new Citizen();
        owner.setUser(users.findByEmail(email(Role.ROLE_CITIZEN, "owner")).orElseThrow());
        citizens.save(owner);
        Citizen otherCitizen = new Citizen();
        otherCitizen.setUser(users.findByEmail(email(Role.ROLE_CITIZEN, "other")).orElseThrow());
        citizens.save(otherCitizen);
        CaseOfficer assigned = new CaseOfficer();
        assigned.setUser(users.findByEmail(email(Role.ROLE_OFFICER, "assigned")).orElseThrow());
        assigned.setEmployeeNumber("CMS-TEST-001");
        officers.save(assigned);
        CaseOfficer replacement = new CaseOfficer();
        replacement.setUser(users.findByEmail(email(Role.ROLE_OFFICER, "other")).orElseThrow());
        replacement.setEmployeeNumber("CMS-TEST-002");
        officers.save(replacement);

        Complaint complaint = new Complaint();
        complaint.setCitizen(owner);
        complaint.setOfficer(assigned);
        complaint.setTitle("Safe isolated fixture");
        complaint.setDescription("No real user data");
        complaint.setCategory("Roads");
        complaint.setAddress("In-memory address");
        complaint.setStatus(ComplaintStatus.ASSIGNED);
        complaint = complaints.save(complaint);
        String path = "/api/complaints/" + complaint.getComplaintId();

        mvc.perform(get(path).header("Authorization", "Bearer " + ownerToken)).andExpect(status().isOk());
        mvc.perform(get(path).header("Authorization", "Bearer " + otherCitizenToken)).andExpect(status().isNotFound());
        mvc.perform(get(path).header("Authorization", "Bearer " + officerToken)).andExpect(status().isOk());
        mvc.perform(get(path).header("Authorization", "Bearer " + otherOfficerToken)).andExpect(status().isNotFound());
        mvc.perform(get(path).header("Authorization", "Bearer " + adminToken)).andExpect(status().isOk());

        String mine = mvc.perform(get("/api/complaints/my").header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String otherMine = mvc.perform(get("/api/complaints/my").header("Authorization", "Bearer " + otherCitizenToken))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertEquals(1, json.readTree(mine).size());
        assertEquals(0, json.readTree(otherMine).size());

        complaint.setOfficer(replacement);
        complaints.saveAndFlush(complaint);
        mvc.perform(get(path).header("Authorization", "Bearer " + officerToken)).andExpect(status().isNotFound());
        mvc.perform(get(path).header("Authorization", "Bearer " + otherOfficerToken)).andExpect(status().isOk());
    }

    @Test
    @Transactional
    void notificationHttpReadsAndReadActionsAreOwnerScoped() throws Exception {
        String ownerToken = tokenFor(Role.ROLE_CITIZEN, "notify-owner");
        String otherToken = tokenFor(Role.ROLE_CITIZEN, "notify-other");
        User owner = users.findByEmail(email(Role.ROLE_CITIZEN, "notify-owner")).orElseThrow();
        Notification first = new Notification();
        first.setUser(owner);
        first.setTitle("Safe fixture update");
        first.setMessage("No real records");
        first = notifications.save(first);
        String readPath = "/api/notifications/" + first.getNotificationId() + "/read";

        String ownerInbox = mvc.perform(get("/api/notifications/my").header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String otherInbox = mvc.perform(get("/api/notifications/my").header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertEquals(1, json.readTree(ownerInbox).size());
        assertEquals(0, json.readTree(otherInbox).size());
        mvc.perform(patch(readPath).header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isNotFound());
        mvc.perform(patch(readPath).header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk());

        Notification second = new Notification();
        second.setUser(owner);
        second.setTitle("Another fixture update");
        second.setMessage("No real records");
        notifications.save(second);
        mvc.perform(patch("/api/notifications/read-all").header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isNoContent());
        String count = mvc.perform(get("/api/notifications/unread-count").header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertEquals(0, json.readTree(count).get("unreadCount").asInt());
    }

    @Test
    @Transactional
    void adminHighPromotionAssignsAndDoesNotRepeatHistoryOrNotifications() throws Exception {
        String adminToken = tokenFor(Role.ROLE_ADMIN, "promotion-admin");
        String citizenToken = tokenFor(Role.ROLE_CITIZEN, "promotion-citizen");
        String officerToken = tokenFor(Role.ROLE_OFFICER, "promotion-officer");
        Citizen citizen = new Citizen();
        citizen.setUser(users.findByEmail(email(Role.ROLE_CITIZEN, "promotion-citizen")).orElseThrow());
        citizens.save(citizen);
        CaseOfficer officer = new CaseOfficer();
        officer.setUser(users.findByEmail(email(Role.ROLE_OFFICER, "promotion-officer")).orElseThrow());
        officer.setEmployeeNumber("CMS-OFF-921");
        officers.save(officer);
        Complaint complaint = new Complaint();
        complaint.setCitizen(citizen);
        complaint.setTitle("Promotion fixture");
        complaint.setDescription("Safe fixture");
        complaint.setCategory("Roads");
        complaint.setAddress("In-memory address");
        complaint.setStatus(ComplaintStatus.SUBMITTED);
        complaint = complaints.saveAndFlush(complaint);
        String path = "/api/admin/complaints/" + complaint.getComplaintId() + "/priority";
        String body = "{\"priority\":\"HIGH\"}";

        mvc.perform(patch(path).header("Authorization", "Bearer " + citizenToken)
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
        mvc.perform(patch(path).header("Authorization", "Bearer " + officerToken)
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
        String response = mvc.perform(patch(path).header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        var promoted = json.readTree(response);
        assertEquals("HIGH", promoted.get("priority").asText());
        assertEquals("ASSIGNED", promoted.get("status").asText());
        assertEquals(officer.getUserId(), promoted.get("officerId").asInt());
        int complaintId = complaint.getComplaintId();
        var entries = histories.findAll().stream()
                .filter(item -> item.getComplaint().getComplaintId().equals(complaintId)).toList();
        assertEquals(2, entries.size());
        assertEquals(1, entries.stream().filter(item -> "HIGH".equals(item.getNewPriority())).count());
        assertEquals(1, entries.stream().filter(item -> "ASSIGNED".equals(item.getNewStatus())).count());
        long notificationsBefore = notifications.findAll().stream()
                .filter(item -> item.getComplaint().getComplaintId().equals(complaintId)).count();
        assertEquals(3, notificationsBefore);

        mvc.perform(patch(path).header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
        assertEquals(2, histories.findAll().stream()
                .filter(item -> item.getComplaint().getComplaintId().equals(complaintId)).count());
        assertEquals(notificationsBefore, notifications.findAll().stream()
                .filter(item -> item.getComplaint().getComplaintId().equals(complaintId)).count());
    }

    @Test
    @Transactional
    void adminHighPromotionWithoutOfficersStaysSubmittedAndAlertsAdmin() throws Exception {
        String adminToken = tokenFor(Role.ROLE_ADMIN, "promotion-empty-admin");
        tokenFor(Role.ROLE_CITIZEN, "promotion-empty-citizen");
        Citizen citizen = new Citizen();
        citizen.setUser(users.findByEmail(email(Role.ROLE_CITIZEN, "promotion-empty-citizen")).orElseThrow());
        citizens.save(citizen);
        Complaint complaint = new Complaint();
        complaint.setCitizen(citizen);
        complaint.setTitle("No officer fixture");
        complaint.setDescription("Safe fixture");
        complaint.setCategory("Roads");
        complaint.setAddress("In-memory address");
        complaint.setStatus(ComplaintStatus.SUBMITTED);
        complaint = complaints.saveAndFlush(complaint);
        int complaintId = complaint.getComplaintId();

        String response = mvc.perform(patch("/api/admin/complaints/" + complaintId + "/priority")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON).content("{\"priority\":\"HIGH\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        var promoted = json.readTree(response);
        assertEquals("HIGH", promoted.get("priority").asText());
        assertEquals("SUBMITTED", promoted.get("status").asText());
        assertEquals(true, promoted.get("officerId").isNull());
        assertEquals(1, histories.findAll().stream()
                .filter(item -> item.getComplaint().getComplaintId().equals(complaintId)).count());
        assertEquals(1, notifications.findAll().stream()
                .filter(item -> item.getComplaint().getComplaintId().equals(complaintId)
                        && "Unassigned HIGH-Priority Complaint".equals(item.getTitle())).count());
    }

    @Test
    @Transactional
    void eachRoleCanUpdateOnlyOwnContactFieldsAndOfficerNumberIsReadOnly() throws Exception {
        for (Role role : Role.values()) {
            String suffix = "profile-" + role.name().toLowerCase();
            String token = tokenFor(role, suffix);
            User account = users.findByEmail(email(role, suffix)).orElseThrow();
            if (role == Role.ROLE_OFFICER) {
                CaseOfficer officer = new CaseOfficer();
                officer.setUser(account);
                officer.setEmployeeNumber("CMS-OFF-931");
                officers.saveAndFlush(officer);
            }
            String initial = mvc.perform(get("/auth/me").header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
            assertEquals(role.name(), json.readTree(initial).get("role").asText());
            assertEquals(account.getUserId(), json.readTree(initial).get("userId").asInt());
            if (role == Role.ROLE_OFFICER) {
                assertEquals("CMS-OFF-931", json.readTree(initial).get("employeeNumber").asText());
            }

            String updated = mvc.perform(patch("/auth/me").header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"phone\":\"+91 1234567890\",\"address\":\"  Test address  \"}"))
                    .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
            assertEquals("+91 1234567890", json.readTree(updated).get("phone").asText());
            assertEquals("Test address", json.readTree(updated).get("address").asText());
            assertEquals(role.name(), json.readTree(updated).get("role").asText());
            assertEquals(account.getEmail(), json.readTree(updated).get("email").asText());
            String refreshed = mvc.perform(get("/auth/me").header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
            assertEquals("Test address", json.readTree(refreshed).get("address").asText());
            assertEquals(role, users.findById(account.getUserId()).orElseThrow().getUserRole());

            mvc.perform(patch("/auth/me").header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON).content("{\"phone\":null,\"address\":\"\"}"))
                    .andExpect(status().isOk());
            User cleared = users.findById(account.getUserId()).orElseThrow();
            assertEquals(null, cleared.getPhone());
            assertEquals(null, cleared.getAddress());
        }
    }

    @Test
    @Transactional
    void profileEndpointRejectsInvalidValuesAndIgnoresClientIdentityClaims() throws Exception {
        String ownerToken = tokenFor(Role.ROLE_CITIZEN, "profile-owner");
        tokenFor(Role.ROLE_CITIZEN, "profile-other");
        User owner = users.findByEmail(email(Role.ROLE_CITIZEN, "profile-owner")).orElseThrow();
        User other = users.findByEmail(email(Role.ROLE_CITIZEN, "profile-other")).orElseThrow();
        mvc.perform(patch("/auth/me").contentType(MediaType.APPLICATION_JSON)
                .content("{\"phone\":\"1234567\",\"address\":\"Test\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(patch("/auth/me").header("Authorization", "Bearer " + ownerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"phone\":\"1234567890123456\",\"address\":\"Test\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(patch("/auth/me").header("Authorization", "Bearer " + ownerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"phone\":\"letters\",\"address\":\"Test\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(patch("/auth/me").header("Authorization", "Bearer " + ownerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"phone\":null,\"address\":\"" + "😀".repeat(16384) + "\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(patch("/auth/me?userId=" + other.getUserId())
                .header("Authorization", "Bearer " + ownerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"phone\":\"1234567\",\"address\":\"Owner address\","
                        + "\"role\":\"ROLE_ADMIN\",\"email\":\"changed@example.test\"}"))
                .andExpect(status().isOk());
        assertEquals("Owner address", users.findById(owner.getUserId()).orElseThrow().getAddress());
        assertEquals(null, users.findById(other.getUserId()).orElseThrow().getAddress());
        assertEquals(Role.ROLE_CITIZEN, users.findById(owner.getUserId()).orElseThrow().getUserRole());
        assertEquals(owner.getEmail(), users.findById(owner.getUserId()).orElseThrow().getEmail());
    }
}
