package com.inventalert.analyticsService.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;

class CompanyAnalyticsControllerIT extends ClickHouseIntegrationTest {

    @Autowired
    WebTestClient webTestClient;

    @Test
    void getCompanySummary_superAdminJwt_returns200() {
        String token = TestJwtHelper.buildSuperAdminToken();

        webTestClient.get()
                .uri("/api/analytics/companies/summary?months=3")
                .headers(h -> h.setBearerAuth(token))
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.OK);
    }

    @Test
    void getCompanySummary_nonSuperAdminJwt_returns403() {
        String token = TestJwtHelper.buildAdminToken("co-test-1");

        webTestClient.get()
                .uri("/api/analytics/companies/summary")
                .headers(h -> h.setBearerAuth(token))
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void getCompanySummary_noJwt_returns401() {
        webTestClient.get()
                .uri("/api/analytics/companies/summary")
                .exchange()
                .expectStatus().isUnauthorized();
    }
}
