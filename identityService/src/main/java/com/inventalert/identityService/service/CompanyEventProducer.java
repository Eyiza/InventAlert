package com.inventalert.identityService.service;

// Stub — Dev B replaces this with the real Kafka implementation in kafka/CompanyEventProducer.java
public interface CompanyEventProducer {
    void publishCompanyCreated(String companyId, String companyName, String adminEmail);
    void publishCompanyOffboarded(String companyId);
}
