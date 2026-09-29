package com.phoenix.business.config;

import com.phoenix.business.domain.*;
import com.phoenix.business.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class BootstrapOwner implements ApplicationRunner {
    private final UserRepository users;
    private final BusinessSettingsRepository settings;
    private final PasswordEncoder encoder;
    private final String username, password, businessName, logoUrl;
    public BootstrapOwner(UserRepository users, BusinessSettingsRepository settings, PasswordEncoder encoder,
            @Value("${app.bootstrap-owner-username}") String username,
            @Value("${app.bootstrap-owner-password:}") String password,
            @Value("${app.bootstrap-business-name:}") String businessName,
            @Value("${app.bootstrap-logo-url:}") String logoUrl) {
        this.users=users; this.settings=settings; this.encoder=encoder; this.username=username;
        this.password=password; this.businessName=businessName; this.logoUrl=logoUrl;
    }
    @Override @Transactional
    public void run(ApplicationArguments args) {
        if (users.count()>0) return;
        if (password==null || !password.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{10,72}$")
                || password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>72)
            throw new IllegalStateException("BOOTSTRAP_OWNER_PASSWORD is required on an empty database and must meet password policy");
        if (!businessName.isBlank()) {
            BusinessSettings s=settings.findById(1L).orElseGet(BusinessSettings::new);
            s.setGymName(businessName.trim()); s.setLogoUrl(logoUrl.isBlank()?null:logoUrl);
            settings.save(s);
        }
        User u=new User(); u.setUsername(username); u.setDisplayName("Gym Owner");
        u.setRole(User.Role.OWNER); u.setActive(true); u.setForcePasswordChange(true);
        u.setPasswordHash(encoder.encode(password)); users.save(u);
    }
}
