package com.phoenix.business.service;

import com.phoenix.business.domain.BusinessSettings;
import com.phoenix.business.repository.BusinessSettingsRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BusinessTimeService {
    private static final ZoneId DEFAULT_ZONE = ZoneId.of("Asia/Kolkata");
    private final BusinessSettingsRepository settings;

    public ZoneId zone() {
        return DEFAULT_ZONE;
    }

    public String format(Instant instant) {
        return instant == null ? "" : DateTimeFormatter.ofPattern("dd MMM uuuu, hh:mm a 'IST (UTC+05:30)'", java.util.Locale.ENGLISH).withZone(zone()).format(instant);
    }

    public LocalDate today() { return LocalDate.now(zone()); }
    public Instant startOfDay(LocalDate date) { return date.atStartOfDay(zone()).toInstant(); }
    public int reminderDays() { return Math.max(0, settings.findById(1L).map(BusinessSettings::getReminderDays).orElse(7)); }
}
