package com.phoenix.business;

import java.util.UUID;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import com.phoenix.business.domain.BusinessSettings;
import com.phoenix.business.repository.BusinessSettingsRepository;

/** Test-classpath-only screenshot launcher with an isolated in-memory database. */
public final class PortfolioDemoApplication {
    public static void main(String[] args) {
        if (!"true".equals(System.getenv("PORTFOLIO_DEMO")))
            throw new IllegalStateException("Use the isolated portfolio screenshot runner");
        new SpringApplication(PhoenixBusinessApplication.class, Fixtures.class).run(
            "--spring.profiles.active=test", "--spring.profiles.include=",
            "--server.address=127.0.0.1", "--server.port=19781",
            "--spring.datasource.url=jdbc:h2:mem:portfolio_" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
            "--spring.datasource.driver-class-name=org.h2.Driver", "--spring.datasource.username=sa", "--spring.datasource.password=",
            "--spring.jpa.hibernate.ddl-auto=create-drop", "--spring.flyway.enabled=false",
            "--app.cors-origins=http://127.0.0.1:4175",
            "--app.jwt-expiration-ms=3600000",
            "--app.bootstrap-owner-username=" + System.getenv("DEMO_USERNAME"),
            "--app.bootstrap-owner-password=" + System.getenv("DEMO_BOOTSTRAP_PASSWORD"));
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class Fixtures {
        @Bean ApplicationRunner demoSettings(BusinessSettingsRepository settings) {
            return args -> {
                if (settings.findById(1L).isEmpty()) {
                    BusinessSettings value = new BusinessSettings();
                    value.setGymName("Phoenix Gym Business");
                    settings.save(value);
                }
            };
        }
    }
}
