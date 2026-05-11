package com.inventalert.inventoryService.kafka;

import com.inventalert.inventoryService.multicompany.CompanySchemaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class CompanyCreatedConsumer {

    private final CompanySchemaService schemaService;

    @KafkaListener(topics = "company.created", groupId = "inventory-service")
    public void consume(Map<String, Object> event) {
        String companyId = (String) event.get("companyId");
        log.info("Provisioning schema for company: {}", companyId);
        schemaService.provisionSchema(companyId);
        log.info("Schema provisioned for company: {}", companyId);
    }
}
