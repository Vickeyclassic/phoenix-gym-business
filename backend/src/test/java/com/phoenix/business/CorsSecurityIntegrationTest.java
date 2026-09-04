package com.phoenix.business;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "app.cors-origins=http://localhost:5173, https://admin.example",
        "spring.datasource.url=jdbc:h2:mem:phoenix-cors;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CorsSecurityIntegrationTest {

    private static final String LOCAL_ORIGIN = "http://localhost:5173";

    @Autowired
    MockMvc mvc;

    @Autowired
    ApplicationContext context;

    @Test
    void configuredLoginPreflightReturnsExpectedCorsHeaders() throws Exception {
        mvc.perform(loginPreflight(LOCAL_ORIGIN))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, LOCAL_ORIGIN))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
    }

    @Test
    void contentTypeLoginPreflightSucceeds() throws Exception {
        mvc.perform(loginPreflight(LOCAL_ORIGIN)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, LOCAL_ORIGIN));
    }

    @Test
    void unconfiguredOriginIsRejected() throws Exception {
        mvc.perform(loginPreflight("https://unconfigured.example"))
                .andExpect(status().isForbidden());
    }

    @Test
    void commaSeparatedConfiguredOriginsAreTrimmedAndAccepted() throws Exception {
        for (String origin : List.of(LOCAL_ORIGIN, "https://admin.example")) {
            mvc.perform(loginPreflight(origin))
                    .andExpect(status().isOk())
                    .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origin));
        }
    }

    @Test
    void applicationContextExposesExpectedCorsBeanName() {
        assertTrue(context.containsBean("corsConfigurationSource"));
        assertInstanceOf(CorsConfigurationSource.class, context.getBean("corsConfigurationSource"));
    }

    private static org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder loginPreflight(String origin) {
        return options("/api/auth/login")
                .header(HttpHeaders.ORIGIN, origin)
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST");
    }
}
