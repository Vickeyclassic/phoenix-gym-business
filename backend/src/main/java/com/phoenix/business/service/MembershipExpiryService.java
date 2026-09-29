package com.phoenix.business.service;

import com.phoenix.business.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @EnableScheduling @RequiredArgsConstructor
public class MembershipExpiryService {
    private final MemberRepository members;
    private final BusinessTimeService businessTime;
    @Scheduled(fixedDelay=60000, initialDelay=60000)
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void expire() { members.expireBefore(businessTime.today()); }
}
