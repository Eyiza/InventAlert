package com.inventalert.identityService.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventalert.identityService.dto.request.SignupRequest;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class CompanyEventProducerIT {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8");

    @Container
    @ServiceConnection
    static KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.6.0"));

    @Autowired MockMvc     mockMvc;
    @Autowired ObjectMapper objectMapper;

    @Test
    void signup_publishesCompanyCreatedEventWithCorrectPayload() throws Exception {
        SignupRequest req = new SignupRequest();
        req.setCompanyName("Flutterwave Ltd");
        req.setAdminEmail("ops@flutter.ng");
        req.setPassword("password123");

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        ConsumerRecord<String, String> record = pollOneRecord("company.created");

        assertThat(record).isNotNull();
        Map<?, ?> payload = objectMapper.readValue(record.value(), Map.class);
        assertThat(payload.get("companyName")).isEqualTo("Flutterwave Ltd");
        assertThat(payload.get("adminEmail")).isEqualTo("ops@flutter.ng");
        assertThat(payload.get("companyId")).isNotNull();
        assertThat(payload.get("eventId")).isNotNull();
        assertThat(payload.get("timestamp")).isNotNull();
        assertThat(record.key()).isEqualTo(payload.get("companyId").toString());
    }

    @Test
    void signup_eventIdIsUniquePerSignup() throws Exception {
        SignupRequest reqA = new SignupRequest();
        reqA.setCompanyName("Aso Rock Tech");
        reqA.setAdminEmail("info@asorock.ng");
        reqA.setPassword("password123");

        SignupRequest reqB = new SignupRequest();
        reqB.setCompanyName("Lekki Startups");
        reqB.setAdminEmail("admin@lekki.ng");
        reqB.setPassword("password123");

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqA)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqB)))
                .andExpect(status().isCreated());

        ConsumerRecord<String, String> first  = pollOneRecord("company.created");
        ConsumerRecord<String, String> second = pollOneRecord("company.created");

        assertThat(first).isNotNull();
        assertThat(second).isNotNull();

        String firstEventId  = (String) objectMapper.readValue(first.value(),  Map.class).get("eventId");
        String secondEventId = (String) objectMapper.readValue(second.value(), Map.class).get("eventId");
        assertThat(firstEventId).isNotEqualTo(secondEventId);
    }

    private ConsumerRecord<String, String> pollOneRecord(String topic) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "test-consumer-" + System.nanoTime());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(Collections.singletonList(topic));
            var records = consumer.poll(Duration.ofSeconds(10));
            return records.isEmpty() ? null : records.iterator().next();
        }
    }
}
