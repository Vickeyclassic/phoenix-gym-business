package com.phoenix.business.web;

import com.phoenix.business.domain.Member;
import com.phoenix.business.domain.MembershipPlan;
import com.phoenix.business.domain.Payment;
import com.phoenix.business.domain.User;
import com.phoenix.business.repository.MemberRepository;
import com.phoenix.business.repository.MembershipPlanRepository;
import com.phoenix.business.repository.PaymentRepository;
import com.phoenix.business.repository.UserRepository;
import com.phoenix.business.service.AuditService;
import com.phoenix.business.service.ReceiptService;
import com.phoenix.business.web.dto.PaymentResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService service;
    private final PaymentRepository repo;
    private final ReceiptService receipts;

    record Renewal(@NotNull Long memberId, @NotNull Long planId, @NotNull Payment.Method method,
                   String referenceNote, BigDecimal amount) {
    }

    @PostMapping("/renew")
    public PaymentResponse renew(@Valid @RequestBody Renewal q, Authentication a) {
        return service.renew(q, a.getName());
    }

    @GetMapping
    public Page<PaymentResponse> list(@RequestParam(required = false) LocalDate from,
                                      @RequestParam(required = false) LocalDate to,
                                      @RequestParam(required = false) Payment.Method method,
                                      @RequestParam(required = false) Long memberId,
                                      @PageableDefault(size = 20, sort = "paymentDate", direction = Sort.Direction.DESC)
                                      Pageable p) {
        ZoneId zone = ZoneOffset.UTC;
        Instant start = (from == null ? LocalDate.of(2000, 1, 1) : from).atStartOfDay(zone).toInstant();
        Instant end = (to == null ? LocalDate.now().plusDays(1) : to.plusDays(1)).atStartOfDay(zone).toInstant();
        return repo.filter(start, end, method, memberId, p).map(PaymentResponse::from);
    }

    @GetMapping(value = "/{id}/receipt.pdf", produces = "application/pdf")
    public ResponseEntity<byte[]> receipt(@PathVariable Long id) throws Exception {
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=receipt-" + id + ".pdf")
                .body(receipts.create(id));
    }
}

@Service
@RequiredArgsConstructor
class PaymentService {
    private final PaymentRepository payments;
    private final MemberRepository members;
    private final MembershipPlanRepository plans;
    private final UserRepository users;
    private final AuditService audit;

    @Transactional
    PaymentResponse renew(PaymentController.Renewal q, String username) {
        Member member = members.findById(q.memberId()).orElseThrow();
        MembershipPlan plan = plans.findById(q.planId()).filter(MembershipPlan::isActive).orElseThrow();
        User user = users.findByUsernameIgnoreCase(username).orElseThrow();
        LocalDate today = LocalDate.now();
        LocalDate start = member.getExpiryDate() != null && member.getExpiryDate().isAfter(today)
                ? member.getExpiryDate().plusDays(1)
                : today;
        LocalDate end = start.plusMonths(plan.getDurationMonths()).minusDays(1);

        Payment payment = new Payment();
        payment.setReceiptNumber("PHX-" + LocalDate.now().getYear() + "-"
                + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        payment.setMember(member);
        payment.setPlan(plan);
        payment.setPlanNameSnapshot(plan.getName());
        payment.setPeriodStart(start);
        payment.setPeriodEnd(end);
        payment.setPaymentDate(Instant.now());
        payment.setAmount(q.amount() == null ? plan.getPrice() : q.amount());
        payment.setMethod(q.method());
        payment.setReferenceNote(q.referenceNote());
        payment.setReceivedBy(user);
        payment = payments.save(payment);

        member.setPlan(plan);
        member.setMembershipStartDate(start);
        member.setExpiryDate(end);
        member.setStatus(Member.Status.ACTIVE);
        members.save(member);

        audit.record("MEMBERSHIP_RENEWED", "MEMBER", member.getId(),
                "Receipt " + payment.getReceiptNumber() + ", plan " + plan.getName());
        audit.record("PAYMENT_CREATED", "PAYMENT", payment.getId(),
                "Receipt " + payment.getReceiptNumber());
        return PaymentResponse.from(payment);
    }
}
