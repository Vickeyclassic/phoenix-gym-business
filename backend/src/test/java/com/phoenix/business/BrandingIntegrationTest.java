package com.phoenix.business;

import com.phoenix.business.domain.BusinessSettings;
import com.phoenix.business.repository.BusinessSettingsRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties="spring.datasource.url=jdbc:h2:mem:branding;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BrandingIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired BusinessSettingsRepository settings;

    @Test void publicBrandingContainsOnlyIdentityAndReflectsSettings() throws Exception {
        BusinessSettings s = new BusinessSettings();
        s.setGymName("B.G. Fitness");
        s.setLogoUrl("/api/public/branding-assets/bg-fitness.jpg");
        s.setEmail("private@example.com");
        settings.save(s);
        mvc.perform(get("/api/public/branding")).andExpect(status().isOk())
                .andExpect(jsonPath("$.gymName").value("B.G. Fitness"))
                .andExpect(jsonPath("$.logoUrl").value(s.getLogoUrl()))
                .andExpect(jsonPath("$.length()").value(2));
        s.setGymName("Another Gym");
        settings.save(s);
        mvc.perform(get("/api/public/branding")).andExpect(jsonPath("$.gymName").value("Another Gym"));
        mvc.perform(get("/api/settings")).andExpect(status().isUnauthorized());
        mvc.perform(put("/api/settings").contentType("application/json").content("{}"))
                .andExpect(status().isUnauthorized());
    }
}
