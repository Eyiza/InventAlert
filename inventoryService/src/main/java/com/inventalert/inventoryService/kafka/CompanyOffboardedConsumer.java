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
public class CompanyOffboardedConsumer {

    private final CompanySchemaService schemaService;

    @KafkaListener(topics = "company.offboarded", groupId = "inventory-service")
    public void consume(Map<String, Object> event) {
        String companyId = (String) event.get("companyId");
        log.info("Dropping schema for offboarded company: {}", companyId);
        schemaService.dropSchema(companyId);
        log.info("Schema dropped for company: {}", companyId);
    }
}
