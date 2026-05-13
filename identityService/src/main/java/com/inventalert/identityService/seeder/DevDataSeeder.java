package com.inventalert.identityService.seeder;

import com.inventalert.identityService.kafka.CompanyEventProducer;
import com.inventalert.identityService.model.*;
import com.inventalert.identityService.repository.CompanyRepository;
import com.inventalert.identityService.repository.UserRepository;
import com.inventalert.identityService.repository.WarehouseAssignmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.seed-data", havingValue = "true")
public class DevDataSeeder implements ApplicationRunner {

    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final WarehouseAssignmentRepository assignmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final CompanyEventProducer companyEventProducer;

    // Fixed warehouse IDs — must match inventoryService DevDataSeeder
    private static final String WH_PHARMAPLUS_LAGOS  = "20000000-0000-0000-0000-000000000001";
    private static final String WH_PHARMAPLUS_ABUJA  = "20000000-0000-0000-0000-000000000002";
    private static final String WH_EKOFRESH_LAGOS    = "20000000-0000-0000-0000-000000000003";
    private static final String WH_EKOFRESH_IBADAN   = "20000000-0000-0000-0000-000000000004";
    private static final String WH_LAGOSLIVING_ISLAND = "20000000-0000-0000-0000-000000000005";
    private static final String WH_LAGOSLIVING_LEKKI = "20000000-0000-0000-0000-000000000006";
    private static final String WH_TECHZONE_IKEJA    = "20000000-0000-0000-0000-000000000007";
    private static final String WH_TECHZONE_ABUJA    = "20000000-0000-0000-0000-000000000008";

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (companyRepository.count() > 0) {
            log.info("[Seeder] Identity data already present — skipping");
            return;
        }
        log.info("[Seeder] Seeding identity data for 4 companies...");
        String pw = passwordEncoder.encode("Password123!");

        seedCompany("10000000-0000-0000-0000-000000000001", "Pharmaplus Nigeria Ltd",  "pharmaplus.ng",     pw, WH_PHARMAPLUS_LAGOS,   WH_PHARMAPLUS_ABUJA);
        seedCompany("10000000-0000-0000-0000-000000000002", "Eko Fresh Market",        "ekofresh.ng",       pw, WH_EKOFRESH_LAGOS,     WH_EKOFRESH_IBADAN);
        seedCompany("10000000-0000-0000-0000-000000000003", "Lagos Living Furniture",  "lagosfurniture.ng", pw, WH_LAGOSLIVING_ISLAND, WH_LAGOSLIVING_LEKKI);
        seedCompany("10000000-0000-0000-0000-000000000004", "TechZone Gadgets",        "techzone.ng",       pw, WH_TECHZONE_IKEJA,     WH_TECHZONE_ABUJA);

        log.info("[Seeder] Identity seed complete — 4 companies, 20 users, warehouse assignments created (password: Password123!)");
    }

    private void seedCompany(String id, String name, String domain, String pw,
                              String primaryWhId, String secondaryWhId) {
        if (companyRepository.existsById(id)) return;
        companyRepository.save(Company.builder()
                .id(id)
                .companyName(name)
                .adminEmail("admin@" + domain)
                .status(CompanyStatus.ACTIVE)
                .build());
        companyEventProducer.publishCompanyCreated(id, name, "admin@" + domain);

        user(id, "admin@"   + domain, Role.ADMIN,               pw, null,          null);
        user(id, "manager@" + domain, Role.MANAGER,             pw, id, primaryWhId);
        user(id, "staff1@"  + domain, Role.WAREHOUSE_STAFF,     pw, id, primaryWhId);
        user(id, "staff2@"  + domain, Role.WAREHOUSE_STAFF,     pw, id, secondaryWhId);
        user(id, "proc@"    + domain, Role.PROCUREMENT_OFFICER, pw, id, primaryWhId);
    }

    private void user(String companyId, String email, Role role, String pw,
                      String assignCompanyId, String warehouseId) {
        User saved = userRepository.save(User.builder()
                .companyId(companyId)
                .email(email)
                .passwordHash(pw)
                .role(role)
                .mustChangePassword(false)
                .build());
        if (warehouseId != null) {
            assignmentRepository.save(WarehouseAssignment.builder()
                    .userId(saved.getId())
                    .companyId(assignCompanyId)
                    .warehouseId(warehouseId)
                    .build());
        }
    }
}
