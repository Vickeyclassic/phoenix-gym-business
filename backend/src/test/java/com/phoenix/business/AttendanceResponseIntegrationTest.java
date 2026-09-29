package com.phoenix.business;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.phoenix.business.domain.Attendance;
import com.phoenix.business.domain.Member;
import com.phoenix.business.domain.MembershipPlan;
import com.phoenix.business.domain.User;
import com.phoenix.business.repository.AttendanceRepository;
import com.phoenix.business.repository.MemberRepository;
import com.phoenix.business.repository.MembershipPlanRepository;
import com.phoenix.business.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties =
        "spring.datasource.url=jdbc:h2:mem:phoenix-attendance;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AttendanceResponseIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired AttendanceRepository attendance;
    @Autowired MemberRepository members;
    @Autowired MembershipPlanRepository plans;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder encoder;

    Member member;
    User owner;

    @BeforeEach
    void setUp() {
        owner = users.findByUsernameIgnoreCase("owner").orElseThrow();
        owner.setForcePasswordChange(false); users.save(owner);
        if (users.findByUsernameIgnoreCase("attendance-staff").isEmpty()) {
            User staff = new User();
            staff.setUsername("attendance-staff");
            staff.setDisplayName("Attendance Staff");
            staff.setRole(User.Role.STAFF);
            staff.setActive(true);
            staff.setForcePasswordChange(false);
            staff.setPasswordHash(encoder.encode("StrongPass1"));
            users.save(staff);
        }

        String suffix = UUID.randomUUID().toString().substring(0, 8);
        MembershipPlan plan = new MembershipPlan();
        plan.setName("Attendance Plan " + suffix);
        plan.setDurationMonths(1);
        plan.setPrice(BigDecimal.TEN);
        plan.setActive(true);
        plan = plans.save(plan);

        member = new Member();
        member.setMemberCode("ATT-" + suffix);
        member.setName("Attendance Member " + suffix);
        member.setJoinDate(LocalDate.now());
        member.setPlan(plan);
        member.setMembershipStartDate(LocalDate.now());
        member.setExpiryDate(LocalDate.now().plusDays(30));
        member.setStatus(Member.Status.ACTIVE);
        member = members.save(member);
    }

    @Test
    void attendanceListReturnsSafeResponseWithPersistedData() throws Exception {
        Attendance persisted = persistCheckedOutAttendance();
        String body = mvc.perform(auth(get("/api/attendance?size=100&from=" + LocalDate.now()
                                + "&to=" + LocalDate.now()), "owner", "StrongOwner1Password"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode response = json.readTree(body);
        JsonNode row = findById(response.path("content"), persisted.getId());
        assertSafeAttendance(row);
    }

    @Test
    void memberAndDateFilteredAttendanceListReturnsSafeResponse() throws Exception {
        Attendance persisted = persistCheckedOutAttendance();
        String body = mvc.perform(auth(get("/api/attendance?size=50&from=" + LocalDate.now()
                                + "&to=" + LocalDate.now() + "&memberId=" + member.getId()),
                        "owner", "StrongOwner1Password"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode row = findById(json.readTree(body).path("content"), persisted.getId());
        assertSafeAttendance(row);
        assertEquals(member.getId().longValue(), row.path("member").path("id").asLong());
    }

    @Test
    void checkInAndCheckOutResponsesAreSafeForStaff() throws Exception {
        String checkInBody = mvc.perform(auth(post("/api/attendance/check-in/" + member.getId()),
                        "attendance-staff", "StrongPass1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CHECKED_IN"))
                .andReturn().getResponse().getContentAsString();
        assertSafeAttendance(json.readTree(checkInBody));

        String checkOutBody = mvc.perform(auth(post("/api/attendance/check-out/" + member.getId()),
                        "attendance-staff", "StrongPass1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CHECKED_OUT"))
                .andReturn().getResponse().getContentAsString();
        assertSafeAttendance(json.readTree(checkOutBody));
    }

    @Test
    void openStatePreventsDuplicateAndInvalidAttendanceActions() throws Exception {
        mvc.perform(auth(get("/api/attendance/open/" + member.getId()), "attendance-staff", "StrongPass1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.open").value(false));
        mvc.perform(auth(post("/api/attendance/check-in/" + member.getId()), "attendance-staff", "StrongPass1"))
                .andExpect(status().isOk());
        mvc.perform(auth(get("/api/attendance/open/" + member.getId()), "attendance-staff", "StrongPass1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.open").value(true));
        mvc.perform(auth(post("/api/attendance/check-in/" + member.getId()), "attendance-staff", "StrongPass1"))
                .andExpect(status().isUnprocessableEntity());
        mvc.perform(auth(post("/api/attendance/check-out/" + member.getId()), "attendance-staff", "StrongPass1"))
                .andExpect(status().isOk());
        mvc.perform(auth(post("/api/attendance/check-out/" + member.getId()), "attendance-staff", "StrongPass1"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void memberProfileAttendanceResponseIsSafe() throws Exception {
        Attendance persisted = persistCheckedOutAttendance();
        String body = mvc.perform(auth(get("/api/members/" + member.getId() + "/profile"),
                        "owner", "StrongOwner1Password"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode row = findById(json.readTree(body).path("recentAttendance"), persisted.getId());
        assertSafeAttendance(row);
    }

    @Test
    void attendanceAuthorizationRulesRemainIntact() throws Exception {
        mvc.perform(get("/api/attendance"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/attendance/check-in/" + member.getId()))
                .andExpect(status().isUnauthorized());
        mvc.perform(auth(get("/api/attendance"), "attendance-staff", "StrongPass1"))
                .andExpect(status().isOk());
    }

    private Attendance persistCheckedOutAttendance() {
        Attendance record = new Attendance();
        record.setMember(member);
        Instant checkIn = LocalDate.now().atTime(12, 0).toInstant(ZoneOffset.UTC);
        record.setCheckedInAt(checkIn);
        record.setCheckedOutAt(checkIn.plusSeconds(3600));
        record.setStatus(Attendance.Status.CHECKED_OUT);
        record.setRecordedBy(owner);
        return attendance.save(record);
    }

    private MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder request,
                                                String username,
                                                String password) throws Exception {
        String loginBody = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("username", username, "password", password))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return request.header("Authorization", "Bearer " + json.readTree(loginBody).path("accessToken").asText());
    }

    private JsonNode findById(JsonNode rows, Long id) {
        for (JsonNode row : rows) {
            if (row.path("id").asLong() == id) return row;
        }
        throw new AssertionError("Attendance response did not contain id " + id);
    }

    private void assertSafeAttendance(JsonNode row) {
        assertNotNull(row);
        assertTrue(row.hasNonNull("id"));
        assertTrue(row.hasNonNull("member"));
        assertTrue(row.hasNonNull("recordedBy"));
        assertTrue(row.path("member").hasNonNull("memberCode"));
        assertTrue(row.path("member").hasNonNull("name"));
        assertTrue(row.path("recordedBy").hasNonNull("displayName"));
        for (String field : new String[]{"username", "passwordHash", "role", "active",
                "forcePasswordChange", "lastLoginAt", "createdAt", "updatedAt"}) {
            assertFalse(row.path("recordedBy").has(field), "recordedBy must not expose " + field);
        }
        assertTrue(row.findValues("passwordHash").isEmpty());
    }
}
