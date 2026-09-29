package com.phoenix.business.repository;

import com.phoenix.business.domain.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    boolean existsByMemberId(Long id);
    @EntityGraph(attributePaths = {"member", "plan", "receivedBy"})
    @Query("select p from Payment p where p.paymentDate>=:from and p.paymentDate<:to and (:method is null or p.method=:method) and (:memberId is null or p.member.id=:memberId)")
    Page<Payment> filter(@Param("from") Instant from, @Param("to") Instant to,
                         @Param("method") Payment.Method method, @Param("memberId") Long memberId, Pageable p);

    Page<Payment> findByPaymentDateBetween(Instant from, Instant to, Pageable p);

    @EntityGraph(attributePaths = {"member", "plan", "receivedBy"})
    List<Payment> findTop10ByOrderByPaymentDateDesc();

    @EntityGraph(attributePaths = {"member", "plan", "receivedBy"})
    List<Payment> findByMemberIdOrderByPaymentDateDesc(Long id);

    @EntityGraph(attributePaths = {"member", "plan", "receivedBy"})
    @Query("select p from Payment p where p.paymentDate>=:from and p.paymentDate<:to order by p.paymentDate asc")
    List<Payment> findByPaymentDateBetweenOrderByPaymentDateAsc(@Param("from") Instant from, @Param("to") Instant to);

    @Query("select coalesce(sum(p.amount),0) from Payment p where p.status='PAID' and p.paymentDate>=:a and p.paymentDate<:b")
    BigDecimal revenue(@Param("a") Instant a, @Param("b") Instant b);

    @Query("select count(p) from Payment p where p.paymentDate>=:a and p.paymentDate<:b")
    long countByPaymentDateBetween(@Param("a") Instant a, @Param("b") Instant b);
}
