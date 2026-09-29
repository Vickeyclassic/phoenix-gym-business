package com.phoenix.business.web;

import com.phoenix.business.domain.Member;
import com.phoenix.business.repository.AttendanceRepository;
import com.phoenix.business.repository.MemberRepository;
import com.phoenix.business.repository.PaymentRepository;
import com.phoenix.business.service.BusinessTimeService;
import com.phoenix.business.service.MembershipExpiryService;
import com.phoenix.business.web.dto.PaymentResponse;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {
    private final MemberRepository members;
    private final PaymentRepository payments;
    private final AttendanceRepository attendance;
    private final MembershipExpiryService expiry;
    private final BusinessTimeService businessTime;

    @GetMapping
    public Map<String, Object> get() {
        expiry.expire();
        LocalDate today = businessTime.today();
        LocalDate month = today.withDayOfMonth(1);
        Instant dayStart = businessTime.startOfDay(today);
        Instant dayEnd = businessTime.startOfDay(today.plusDays(1));
        Instant monthStart = businessTime.startOfDay(month);
        int reminderDays = businessTime.reminderDays();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("businessDate", today);
        result.put("businessTimeZone", businessTime.zone().getId());
        result.put("expiringSoonDays", reminderDays);
        result.put("totalMembers", members.countByArchivedFalse());
        result.put("activeMembers", members.countByArchivedFalseAndStatus(Member.Status.ACTIVE));
        result.put("expiredMembers", members.countByArchivedFalseAndStatus(Member.Status.EXPIRED));
        result.put("expiringSoon", reminderDays == 0 ? 0 :
                members.countByArchivedFalseAndStatusAndExpiryDateBetween(
                        Member.Status.ACTIVE, today.plusDays(1), today.plusDays(reminderDays)));
        result.put("todayAttendance", attendance.countByCheckedInAtBetween(dayStart, dayEnd));
        result.put("todayRevenue", payments.revenue(dayStart, dayEnd));
        result.put("monthRevenue", payments.revenue(monthStart, dayEnd));
        result.put("newMembersThisMonth", members.countByJoinDateBetween(month, today));
        result.put("renewalsThisMonth", payments.countByPaymentDateBetween(monthStart, dayEnd));
        result.put("recentPayments", payments.findTop10ByOrderByPaymentDateDesc().stream().map(PaymentResponse::from).toList());
        result.put("monthlyRevenue", monthlyRevenue(today));
        result.put("attendanceTrend", attendanceTrend(today));
        result.put("membershipDistribution", members.membershipDistribution().stream()
                .map(row -> Map.of("label", row[0], "value", row[1])).toList());
        return result;
    }

    private List<Map<String, Object>> monthlyRevenue(LocalDate today) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            LocalDate from = today.withDayOfMonth(1).minusMonths(i);
            LocalDate to = from.plusMonths(1);
            out.add(Map.of("label", from.getMonth().toString().substring(0, 3), "value",
                    payments.revenue(businessTime.startOfDay(from), businessTime.startOfDay(to))));
        }
        return out;
    }

    private List<Map<String, Object>> attendanceTrend(LocalDate today) {
        Instant from = businessTime.startOfDay(today.minusDays(6));
        Instant to = businessTime.startOfDay(today.plusDays(1));
        Map<LocalDate, Long> counts = attendance.findByCheckedInAtBetween(from, to).stream()
                .collect(Collectors.groupingBy(
                        row -> row.getCheckedInAt().atZone(businessTime.zone()).toLocalDate(),
                        Collectors.counting()));
        List<Map<String, Object>> out = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            out.add(Map.of("label", date.getDayOfWeek().toString().substring(0, 3),
                    "value", counts.getOrDefault(date, 0L)));
        }
        return out;
    }
}
