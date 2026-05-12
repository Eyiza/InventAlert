package com.inventalert.analyticsService.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;

class StockAnalyticsControllerIT extends ClickHouseIntegrationTest {

    @Autowired
    WebTestClient webTestClient;

    @Test
    void getStockSummary_withManagerJwt_returns200() {
        String token = TestJwtHelper.buildManagerToken("co-test-1");

        webTestClient.get()
                .uri("/api/analytics/stock/summary")
                .headers(h -> h.setBearerAuth(token))
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.OK);
    }

    @Test
    void getStockSummary_withAdminJwt_returns200() {
        String token = TestJwtHelper.buildAdminToken("co-test-1");

        webTestClient.get()
                .uri("/api/analytics/stock/summary")
                .headers(h -> h.setBearerAuth(token))
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.OK);
    }

    @Test
    void getStockSummary_noJwt_returns401() {
        webTestClient.get()
                .uri("/api/analytics/stock/summary")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void getStockSummary_warehouseStaffJwt_returns403() {
        String token = TestJwtHelper.buildWarehouseStaffToken("co-test-1");

        webTestClient.get()
                .uri("/api/analytics/stock/summary")
                .headers(h -> h.setBearerAuth(token))
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void getStockSummary_fromAfterTo_returns400() {
        String token = TestJwtHelper.buildManagerToken("co-test-1");

        webTestClient.get()
                .uri("/api/analytics/stock/summary?from=2025-12-01T00:00:00Z&to=2025-01-01T00:00:00Z")
                .headers(h -> h.setBearerAuth(token))
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void getStockSummary_rangeExceedsOneYear_returns400() {
        String token = TestJwtHelper.buildManagerToken("co-test-1");

        webTestClient.get()
                .uri("/api/analytics/stock/summary?from=2020-01-01T00:00:00Z&to=2025-01-01T00:00:00Z")
                .headers(h -> h.setBearerAuth(token))
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void getTopProducts_withManagerJwt_returns200() {
        String token = TestJwtHelper.buildManagerToken("co-test-1");

        webTestClient.get()
                .uri("/api/analytics/stock/top-products?type=OUTBOUND_SALE&limit=5")
                .headers(h -> h.setBearerAuth(token))
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.OK);
    }
}
