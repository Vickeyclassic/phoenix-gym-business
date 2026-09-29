package com.phoenix.business.web;

import com.phoenix.business.domain.Attendance;
import com.phoenix.business.domain.Member;
import com.phoenix.business.repository.AttendanceRepository;
import com.phoenix.business.repository.MemberRepository;
import com.phoenix.business.repository.UserRepository;
import com.phoenix.business.service.AuditService;
import com.phoenix.business.service.BusinessTimeService;
import com.phoenix.business.web.dto.AttendanceResponse;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
public class AttendanceController {
    private final AttendanceRepository repo;
    private final MemberRepository members;
    private final UserRepository users;
    private final AuditService audit;
    private final BusinessTimeService businessTime;

    @PostMapping("/check-in/{memberId}")
    @Transactional
    public AttendanceResponse in(@PathVariable Long memberId, Authentication auth) {
        Member member = members.findLockedById(memberId).orElseThrow();
        if (member.isArchived() || member.getStatus() != Member.Status.ACTIVE || member.getExpiryDate() == null
                || member.getExpiryDate().isBefore(businessTime.today())) {
            throw new IllegalArgumentException("Only active, unexpired members may check in");
        }
        if (repo.findFirstByMemberIdAndStatusOrderByCheckedInAtDesc(memberId, Attendance.Status.CHECKED_IN).isPresent()) {
            throw new IllegalArgumentException("Member already has an open check-in");
        }
        Attendance row = new Attendance();
        row.setMember(member);
        row.setCheckedInAt(Instant.now());
        row.setRecordedBy(users.findByUsernameIgnoreCase(auth.getName()).orElseThrow());
        row = repo.save(row);
        audit.record("ATTENDANCE_CHECK_IN", "ATTENDANCE", row.getId(), member.getMemberCode());
        return AttendanceResponse.from(row);
    }

    @PostMapping("/check-out/{memberId}")
    @Transactional
    public AttendanceResponse out(@PathVariable Long memberId) {
        Attendance row = repo.findFirstByMemberIdAndStatusOrderByCheckedInAtDesc(memberId, Attendance.Status.CHECKED_IN)
                .orElseThrow(() -> new IllegalArgumentException("No open check-in for this member"));
        row.setCheckedOutAt(Instant.now());
        row.setStatus(Attendance.Status.CHECKED_OUT);
        row = repo.save(row);
        audit.record("ATTENDANCE_CHECK_OUT", "ATTENDANCE", row.getId(), row.getMember().getMemberCode());
        return AttendanceResponse.from(row);
    }

    @GetMapping("/open/{memberId}")
    public Map<String, Object> open(@PathVariable Long memberId) {
        Member member = members.findById(memberId).orElseThrow();
        if (member.isArchived()) throw new IllegalArgumentException("Archived members cannot record attendance");
        var open = repo.findFirstByMemberIdAndStatusOrderByCheckedInAtDesc(memberId, Attendance.Status.CHECKED_IN);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("open", open.isPresent());
        open.ifPresent(row -> result.put("attendance", AttendanceResponse.from(row)));
        return result;
    }

    @GetMapping
    public Page<AttendanceResponse> list(@RequestParam(required = false) LocalDate from,
                                         @RequestParam(required = false) LocalDate to,
                                         @RequestParam(required = false) Long memberId,
                                         @PageableDefault(size = 20, sort = "checkedInAt",
                                                 direction = Sort.Direction.DESC) Pageable pageable) {
        LocalDate start = from == null ? businessTime.today() : from;
        LocalDate end = to == null ? start : to;
        if (end.isBefore(start)) throw new IllegalArgumentException("To date must be on or after from date");
        return repo.filter(businessTime.startOfDay(start), businessTime.startOfDay(end.plusDays(1)), memberId, pageable)
                .map(AttendanceResponse::from);
    }
}
