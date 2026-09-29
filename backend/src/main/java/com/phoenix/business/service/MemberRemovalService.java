package com.phoenix.business.service;

import com.phoenix.business.domain.Member;
import com.phoenix.business.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class MemberRemovalService {
    private final MemberRepository members;
    private final PaymentRepository payments;
    private final AttendanceRepository attendance;
    private final AuditService audit;
    private final PlatformTransactionManager transactions;
    private final BusinessTimeService businessTime;

    public String remove(Long id) {
        TransactionTemplate tx = new TransactionTemplate(transactions);
        try {
            return tx.execute(status -> {
                Member member = members.findLockedById(id).orElseThrow();
                // Financial and attendance records are permanent audit history.
                if (member.isArchived() || payments.existsByMemberId(id) || attendance.existsByMemberId(id)) {
                    return archive(member);
                }
                members.delete(member);
                members.flush();
                audit.record("MEMBER_DELETED", "MEMBER", id, member.getMemberCode());
                return "deleted";
            });
        } catch (DataIntegrityViolationException conflict) {
            // The failed delete transaction is rolled back before preserving any other references.
            return tx.execute(status -> archive(members.findLockedById(id).orElseThrow()));
        }
    }

    private String archive(Member member) {
        if (!member.isArchived()) {
            member.setArchived(true);
            members.saveAndFlush(member);
            audit.record("MEMBER_ARCHIVED", "MEMBER", member.getId(), member.getMemberCode());
        }
        return "archived";
    }

    public Member restore(Long id) {
        return new TransactionTemplate(transactions).execute(status -> {
            Member member = members.findLockedById(id).orElseThrow();
            if (member.isArchived()) {
                member.setArchived(false);
                if (member.getStatus() == Member.Status.ACTIVE && member.getExpiryDate() != null
                        && member.getExpiryDate().isBefore(businessTime.today())) member.setStatus(Member.Status.EXPIRED);
                members.saveAndFlush(member);
                audit.record("MEMBER_RESTORED", "MEMBER", id, member.getMemberCode());
            }
            return member;
        });
    }
}
