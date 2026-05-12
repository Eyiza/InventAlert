package com.inventalert.analyticsService.integration;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class ClickHouseIntegrationTest {

    @Container
    static GenericContainer<?> clickhouse = new GenericContainer<>("clickhouse/clickhouse-server:24.3")
            .withExposedPorts(8123)
            .withCopyFileToContainer(
                    MountableFile.forClasspathResource("clickhouse-test-init.sql"),
                    "/docker-entrypoint-initdb.d/init.sql");

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("clickhouse.host", clickhouse::getHost);
        registry.add("clickhouse.http-port", () -> clickhouse.getMappedPort(8123));
        registry.add("clickhouse.database", () -> "inventalert_analytics");
        registry.add("clickhouse.username", () -> "default");
        registry.add("clickhouse.password", () -> "");
        registry.add("jwt.secret", () -> "test-secret-key-minimum-32-chars!!");
        // Disable Kafka auto-connect for ClickHouse-only tests
        registry.add("spring.kafka.bootstrap-servers", () -> "localhost:9092");
        registry.add("spring.autoconfigure.exclude",
                () -> "org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration");
    }
}
