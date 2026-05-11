package com.inventalert.notificationService;

import com.inventalert.notificationService.repository.RedisNotificationRepository;
import com.inventalert.notificationService.service.EmailService;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class NotificationEventConsumerIT {

    @Container
    static KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.6.0"));

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    private static KafkaProducer<String, String> producer;

    @BeforeAll
    static void startProducer() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producer = new KafkaProducer<>(props);
    }

    @AfterAll
    static void stopProducer() {
        if (producer != null) producer.close();
    }

    @Autowired RedisNotificationRepository repository;
    @Autowired StringRedisTemplate redisTemplate;

    @MockitoBean EmailService emailService;
    @MockitoBean SimpMessagingTemplate messagingTemplate;

    @BeforeEach
    void setUp() {
        redisTemplate.execute((RedisCallback<Void>) connection -> {
            connection.serverCommands().flushDb();
            return null;
        });
    }

    @Test
    void Consume_PublishedRestockAlert_CheckIfNotificationStoredInRedisTest() throws Exception {
        String message = """
                {"eventId":"evt-kafka-001","companyId":"konga-001","userId":"adebayo-001",
                 "userEmail":"adebayo@konga.ng","type":"RESTOCK_ALERT",
                 "message":"Low stock: Indomie noodles","referenceId":"product-001"}
                """;

        producer.send(new ProducerRecord<>("notification.events", message)).get(5, TimeUnit.SECONDS);

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            Set<String> ids = repository.getNotificationIds("konga-001", "adebayo-001", 0, 10);
            assertThat(ids).isNotEmpty();
        });

        Set<String> ids = repository.getNotificationIds("konga-001", "adebayo-001", 0, 10);
        String id = ids.iterator().next();
        var hash = repository.getHash("konga-001", id);

        assertThat(hash.get("type")).isEqualTo("RESTOCK_ALERT");
        assertThat(hash.get("message")).isEqualTo("Low stock: Indomie noodles");
        assertThat(hash.get("isRead")).isEqualTo("0");
    }

    @Test
    void Consume_DuplicateEventId_CheckIfOnlyOneNotificationStoredTest() throws Exception {
        String message = """
                {"eventId":"evt-kafka-dup","companyId":"stanbic-001","userId":"ngozi-001",
                 "userEmail":"ngozi@stanbic.ng","type":"TRANSFER_SUGGESTION",
                 "message":"Transfer suggested for Apapa warehouse","referenceId":"transfer-001"}
                """;

        producer.send(new ProducerRecord<>("notification.events", message)).get(5, TimeUnit.SECONDS);
        producer.send(new ProducerRecord<>("notification.events", message)).get(5, TimeUnit.SECONDS);

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            Set<String> ids = repository.getNotificationIds("stanbic-001", "ngozi-001", 0, 10);
            assertThat(ids).isNotEmpty();
        });

        Set<String> ids = repository.getNotificationIds("stanbic-001", "ngozi-001", 0, 10);
        assertThat(ids).hasSize(1);
    }

    @Test
    void Consume_MalformedMessage_CheckIfOtherMessagesStillProcessedTest() throws Exception {
        producer.send(new ProducerRecord<>("notification.events", "{invalid json}")).get(5, TimeUnit.SECONDS);
        producer.send(new ProducerRecord<>("notification.events", """
                {"eventId":"evt-kafka-003","companyId":"fidelity-001","userId":"emeka-003",
                 "userEmail":"emeka@fidelity.ng","type":"TRANSFER_APPROVED",
                 "message":"Transfer approved","referenceId":"transfer-003"}
                """)).get(5, TimeUnit.SECONDS);

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            Set<String> ids = repository.getNotificationIds("fidelity-001", "emeka-003", 0, 10);
            assertThat(ids).isNotEmpty();
        });
    }
}
