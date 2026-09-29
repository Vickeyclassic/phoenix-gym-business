package com.phoenix.business;

import com.phoenix.business.domain.Attendance;
import com.phoenix.business.domain.BusinessSettings;
import com.phoenix.business.domain.Member;
import com.phoenix.business.repository.AttendanceRepository;
import com.phoenix.business.repository.BusinessSettingsRepository;
import com.phoenix.business.repository.MemberRepository;
import com.phoenix.business.service.BusinessTimeService;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties =
        "spring.datasource.url=jdbc:h2:mem:membership-dashboard;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser(username = "owner", roles = "OWNER")
class MembershipDashboardIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired MemberRepository members;
    @Autowired AttendanceRepository attendance;
    @Autowired BusinessSettingsRepository settings;
    @Autowired BusinessTimeService businessTime;

    @BeforeEach
    void setUp() {
        attendance.deleteAll();
        members.deleteAll();
        BusinessSettings business = settings.findById(1L).orElseGet(() -> {
            BusinessSettings created = new BusinessSettings();
            created.setId(1L);
            created.setGymName("B.G. Fitness");
            return created;
        });
        business.setTimeZone("Asia/Kolkata");
        business.setReminderDays(7);
        settings.save(business);
    }

    @Test
    void dashboardReconcilesDirectoryCountsAndUsesConfiguredFutureWindow() throws Exception {
        LocalDate today = businessTime.today();
        member("ACTIVE", Member.Status.ACTIVE, today.plusDays(30), false);
        Member stale = member("STALE", Member.Status.ACTIVE, today.minusDays(1), false);
        member("SOON", Member.Status.ACTIVE, today.plusDays(7), false);
        member("TODAY", Member.Status.ACTIVE, today, false);
        member("INACTIVE", Member.Status.INACTIVE, today.plusDays(2), false);
        member("ARCHIVED", Member.Status.ACTIVE, today.plusDays(2), true);

        mvc.perform(get("/api/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.businessTimeZone").value("Asia/Kolkata"))
                .andExpect(jsonPath("$.expiringSoonDays").value(7))
                .andExpect(jsonPath("$.totalMembers").value(5))
                .andExpect(jsonPath("$.activeMembers").value(3))
                .andExpect(jsonPath("$.expiredMembers").value(1))
                .andExpect(jsonPath("$.expiringSoon").value(1));

        mvc.perform(get("/api/members?size=50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(5));
        mvc.perform(get("/api/members?q=" + stale.getMemberCode()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("EXPIRED"));
    }

    @Test
    void attendanceDayFiltersUseKolkataBoundaries() throws Exception {
        LocalDate today = businessTime.today();
        Member member = member("BOUNDARY", Member.Status.ACTIVE, today.plusDays(30), false);
        Attendance row = new Attendance();
        row.setMember(member);
        row.setCheckedInAt(businessTime.startOfDay(today).plusSeconds(30));
        attendance.save(row);

        mvc.perform(get("/api/attendance?from=" + today + "&to=" + today + "&memberId=" + member.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get("/api/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.todayAttendance").value(1));
    }

    private Member member(String label, Member.Status status, LocalDate expiry, boolean archived) {
        Member member = new Member();
        member.setMemberCode(label + "-" + UUID.randomUUID().toString().substring(0, 6));
        member.setName(label);
        member.setJoinDate(businessTime.today());
        member.setStatus(status);
        member.setExpiryDate(expiry);
        member.setArchived(archived);
        return members.save(member);
    }
}
