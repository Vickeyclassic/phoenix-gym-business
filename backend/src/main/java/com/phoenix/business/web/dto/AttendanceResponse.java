package com.phoenix.business.web.dto;

import com.phoenix.business.domain.Attendance;
import com.phoenix.business.domain.Member;
import com.phoenix.business.domain.User;

import java.time.Instant;

public record AttendanceResponse(
        Long id,
        MemberSummary member,
        Instant checkedInAt,
        Instant checkedOutAt,
        Attendance.Status status,
        String source,
        UserSummary recordedBy,
        Instant createdAt) {

    public static AttendanceResponse from(Attendance attendance) {
        return new AttendanceResponse(
                attendance.getId(),
                MemberSummary.from(attendance.getMember()),
                attendance.getCheckedInAt(),
                attendance.getCheckedOutAt(),
                attendance.getStatus(),
                attendance.getSource(),
                UserSummary.from(attendance.getRecordedBy()),
                attendance.getCreatedAt());
    }

    public record MemberSummary(Long id, String memberCode, String name) {
        private static MemberSummary from(Member member) {
            return member == null ? null : new MemberSummary(member.getId(), member.getMemberCode(), member.getName());
        }
    }

    public record UserSummary(Long id, String displayName) {
        private static UserSummary from(User user) {
            return user == null ? null : new UserSummary(user.getId(), user.getDisplayName());
        }
    }
}
