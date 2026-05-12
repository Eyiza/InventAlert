package com.inventalert.analyticsService.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;

class AlertAnalyticsControllerIT extends ClickHouseIntegrationTest {

    @Autowired
    WebTestClient webTestClient;

    @Test
    void getAlertSummary_managerJwt_returns200() {
        String token = TestJwtHelper.buildManagerToken("co-alert-test");

        webTestClient.get()
                .uri("/api/analytics/alerts/summary")
                .headers(h -> h.setBearerAuth(token))
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.OK);
    }

    @Test
    void getAlertSummary_procurementOfficerJwt_returns200() {
        String token = TestJwtHelper.buildToken("user-po", "co-alert-test", "PROCUREMENT_OFFICER");

        webTestClient.get()
                .uri("/api/analytics/alerts/summary")
                .headers(h -> h.setBearerAuth(token))
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.OK);
    }

    @Test
    void getAlertSummary_warehouseStaffJwt_returns403() {
        String token = TestJwtHelper.buildWarehouseStaffToken("co-alert-test");

        webTestClient.get()
                .uri("/api/analytics/alerts/summary")
                .headers(h -> h.setBearerAuth(token))
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.FORBIDDEN);
    }
}
