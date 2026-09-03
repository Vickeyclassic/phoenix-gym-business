package com.phoenix.business.web.dto;

import com.phoenix.business.domain.Member;
import com.phoenix.business.domain.MembershipPlan;
import com.phoenix.business.domain.Payment;
import com.phoenix.business.domain.User;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record PaymentResponse(
        Long id,
        String receiptNumber,
        MemberSummary member,
        PlanSummary plan,
        String planNameSnapshot,
        LocalDate periodStart,
        LocalDate periodEnd,
        Instant paymentDate,
        BigDecimal amount,
        Payment.Method method,
        String referenceNote,
        Payment.Status status,
        UserSummary receivedBy,
        Instant createdAt) {

    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getReceiptNumber(),
                MemberSummary.from(payment.getMember()),
                PlanSummary.from(payment.getPlan()),
                payment.getPlanNameSnapshot(),
                payment.getPeriodStart(),
                payment.getPeriodEnd(),
                payment.getPaymentDate(),
                payment.getAmount(),
                payment.getMethod(),
                payment.getReferenceNote(),
                payment.getStatus(),
                UserSummary.from(payment.getReceivedBy()),
                payment.getCreatedAt());
    }

    public record MemberSummary(Long id, String memberCode, String name) {
        private static MemberSummary from(Member member) {
            return member == null ? null : new MemberSummary(member.getId(), member.getMemberCode(), member.getName());
        }
    }

    public record PlanSummary(Long id, String name) {
        private static PlanSummary from(MembershipPlan plan) {
            return plan == null ? null : new PlanSummary(plan.getId(), plan.getName());
        }
    }

    public record UserSummary(Long id, String displayName) {
        private static UserSummary from(User user) {
            return user == null ? null : new UserSummary(user.getId(), user.getDisplayName());
        }
    }
}
