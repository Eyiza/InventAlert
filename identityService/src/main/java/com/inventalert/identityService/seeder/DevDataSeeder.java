package com.inventalert.identityService.seeder;

import com.inventalert.identityService.model.*;
import com.inventalert.identityService.repository.CompanyRepository;
import com.inventalert.identityService.repository.UserRepository;
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
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (companyRepository.count() > 0) {
            log.info("[Seeder] Identity data already present — skipping");
            return;
        }
        log.info("[Seeder] Seeding identity data for 4 companies...");
        String pw = passwordEncoder.encode("Password123!");

        seedCompany("10000000-0000-0000-0000-000000000001", "Pharmaplus Nigeria Ltd",  "pharmaplus.ng",      pw);
        seedCompany("10000000-0000-0000-0000-000000000002", "Eko Fresh Market",         "ekofresh.ng",        pw);
        seedCompany("10000000-0000-0000-0000-000000000003", "Lagos Living Furniture",   "lagosfurniture.ng",  pw);
        seedCompany("10000000-0000-0000-0000-000000000004", "TechZone Gadgets",         "techzone.ng",        pw);

        log.info("[Seeder] Identity seed complete — 4 companies, 20 users (password: Password123!)");
    }

    private void seedCompany(String id, String name, String domain, String pw) {
        if (companyRepository.existsById(id)) return;
        companyRepository.save(Company.builder()
                .id(id)
                .companyName(name)
                .adminEmail("admin@" + domain)
                .status(CompanyStatus.ACTIVE)
                .build());

        user(id, "admin@"   + domain, Role.ADMIN,               pw);
        user(id, "manager@" + domain, Role.MANAGER,             pw);
        user(id, "staff1@"  + domain, Role.WAREHOUSE_STAFF,     pw);
        user(id, "staff2@"  + domain, Role.WAREHOUSE_STAFF,     pw);
        user(id, "proc@"    + domain, Role.PROCUREMENT_OFFICER, pw);
    }

    private void user(String companyId, String email, Role role, String pw) {
        userRepository.save(User.builder()
                .companyId(companyId)
                .email(email)
                .passwordHash(pw)
                .role(role)
                .build());
    }
}
