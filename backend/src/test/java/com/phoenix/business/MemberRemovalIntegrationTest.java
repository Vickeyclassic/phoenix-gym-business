package com.phoenix.business;

import com.phoenix.business.domain.*;
import com.phoenix.business.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import java.time.*;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties="spring.datasource.url=jdbc:h2:mem:member-removal;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser(username="owner",roles="OWNER")
class MemberRemovalIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired MemberRepository members;
    @Autowired AttendanceRepository attendance;
    @Autowired PaymentRepository payments;
    @Autowired UserRepository users;

    Member member() {
        Member m=new Member();
        m.setMemberCode("DEL-"+UUID.randomUUID().toString().substring(0,8));
        m.setName(m.getMemberCode());m.setJoinDate(LocalDate.now());
        return members.save(m);
    }

    @Test void deletesMemberWithoutHistory() throws Exception {
        Member m=member();
        mvc.perform(delete("/api/members/"+m.getId())).andExpect(status().isOk()).andExpect(jsonPath("$.outcome").value("deleted"));
        assertFalse(members.existsById(m.getId()));
    }

    @Test void archivesAttendanceHistoryAndRestoresWithoutLosingRecords() throws Exception {
        Member m=member();
        Attendance a=new Attendance();a.setMember(m);a.setCheckedInAt(Instant.now());a=attendance.save(a);
        mvc.perform(delete("/api/members/"+m.getId())).andExpect(status().isOk()).andExpect(jsonPath("$.outcome").value("archived"));
        assertTrue(attendance.existsById(a.getId()));
        mvc.perform(get("/api/members?q="+m.getMemberCode())).andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/api/members?archived=true&q="+m.getMemberCode())).andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(post("/api/attendance/check-in/"+m.getId())).andExpect(status().isUnprocessableEntity());
        mvc.perform(get("/api/members/"+m.getId()+"/profile")).andExpect(status().isOk()).andExpect(jsonPath("$.recentAttendance.length()").value(1));
        mvc.perform(post("/api/members/"+m.getId()+"/restore")).andExpect(status().isOk()).andExpect(jsonPath("$.archived").value(false));
        mvc.perform(get("/api/members?q="+m.getMemberCode())).andExpect(jsonPath("$.totalElements").value(1));
        assertTrue(attendance.existsById(a.getId()));
    }

    @Test void archivesPaymentHistoryAndPreservesPayment() throws Exception {
        Member m=member();Payment p=new Payment();p.setMember(m);p.setReceiptNumber(UUID.randomUUID().toString());
        p.setPlanNameSnapshot("Historical plan");p.setPeriodStart(LocalDate.now());p.setPeriodEnd(LocalDate.now().plusDays(30));
        p.setPaymentDate(Instant.now());p.setAmount(java.math.BigDecimal.TEN);p.setMethod(Payment.Method.CASH);
        p.setReceivedBy(users.findByUsernameIgnoreCase("owner").orElseThrow());p=payments.save(p);
        mvc.perform(delete("/api/members/"+m.getId())).andExpect(status().isOk()).andExpect(jsonPath("$.outcome").value("archived"));
        assertTrue(payments.existsById(p.getId()));
        mvc.perform(get("/api/members/"+m.getId()+"/profile")).andExpect(status().isOk()).andExpect(jsonPath("$.payments.length()").value(1));
    }

    @Test void restoreDoesNotReactivateExpiredMembership() throws Exception {
        Member m=member();m.setExpiryDate(LocalDate.now().minusDays(1));members.save(m);
        Attendance history=new Attendance();history.setMember(m);history.setCheckedInAt(Instant.now());attendance.save(history);
        mvc.perform(delete("/api/members/"+m.getId())).andExpect(jsonPath("$.outcome").value("archived"));
        mvc.perform(post("/api/members/"+m.getId()+"/restore")).andExpect(jsonPath("$.status").value("EXPIRED"));
    }

    @Test @WithMockUser(username="staff",roles="STAFF") void staffCannotRemoveOrRestore() throws Exception {
        Member m=member();
        mvc.perform(delete("/api/members/"+m.getId())).andExpect(status().isForbidden());
        mvc.perform(post("/api/members/"+m.getId()+"/restore")).andExpect(status().isForbidden());
        assertTrue(members.existsById(m.getId()));
    }
}
