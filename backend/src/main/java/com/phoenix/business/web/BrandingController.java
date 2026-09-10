package com.phoenix.business.web;

import com.phoenix.business.repository.BusinessSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class BrandingController {
    private final BusinessSettingsRepository settings;
    public record Branding(String gymName, String logoUrl) {}

    @GetMapping("/api/public/branding")
    public Branding get() {
        return settings.findById(1L)
                .map(s -> new Branding(s.getGymName(), s.getLogoUrl()))
                .orElse(new Branding("Phoenix Business", null));
    }
}
