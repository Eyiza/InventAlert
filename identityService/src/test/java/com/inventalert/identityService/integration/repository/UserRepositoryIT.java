package com.inventalert.identityService.integration.repository;

import com.inventalert.identityService.model.Company;
import com.inventalert.identityService.model.Role;
import com.inventalert.identityService.model.User;
import com.inventalert.identityService.repository.CompanyRepository;
import com.inventalert.identityService.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class UserRepositoryIT {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8");

    @Autowired UserRepository    userRepository;
    @Autowired CompanyRepository companyRepository;

    private Company companyA;
    private Company companyB;

    @BeforeEach
    void setup() {
        userRepository.deleteAll();
        companyRepository.deleteAll();

        companyA = companyRepository.save(Company.builder()
                .companyName("Company A")
                .adminEmail("admin@companya.com")
                .build());

        companyB = companyRepository.save(Company.builder()
                .companyName("Company B")
                .adminEmail("admin@companyb.com")
                .build());
    }

    // ── findByCompanyIdAndEmail ───────────────────────────────────────────────

    @Test
    void findByCompanyIdAndEmail_returnsMatchingUser() {
        userRepository.save(buildUser("user@companya.com", companyA.getId()));

        Optional<User> result = userRepository.findByCompanyIdAndEmail(
                companyA.getId(), "user@companya.com");

        assertThat(result).isPresent();
        assertThat(result.get().getEmail()).isEqualTo("user@companya.com");
        assertThat(result.get().getCompanyId()).isEqualTo(companyA.getId());
    }

    @Test
    void findByCompanyIdAndEmail_wrongCompany_returnsEmpty() {
        userRepository.save(buildUser("user@companya.com", companyA.getId()));

        Optional<User> result = userRepository.findByCompanyIdAndEmail(
                companyB.getId(), "user@companya.com");

        assertThat(result).isEmpty();
    }

    // ── findAllByCompanyId ────────────────────────────────────────────────────

    @Test
    void findAllByCompanyId_returnsOnlyThatCompanysUsers() {
        userRepository.save(buildUser("u1@companya.com", companyA.getId()));
        userRepository.save(buildUser("u2@companya.com", companyA.getId()));
        userRepository.save(buildUser("u1@companyb.com", companyB.getId()));

        List<User> results = userRepository.findAllByCompanyId(companyA.getId());

        assertThat(results).hasSize(2);
        assertThat(results).extracting(User::getEmail)
                .containsExactlyInAnyOrder("u1@companya.com", "u2@companya.com");
    }

    @Test
    void findAllByCompanyId_noUsers_returnsEmptyList() {
        List<User> results = userRepository.findAllByCompanyId(companyA.getId());
        assertThat(results).isEmpty();
    }

    // ── existsByCompanyIdAndEmail ─────────────────────────────────────────────

    @Test
    void existsByCompanyIdAndEmail_existingUser_returnsTrue() {
        userRepository.save(buildUser("check@companya.com", companyA.getId()));

        assertThat(userRepository.existsByCompanyIdAndEmail(companyA.getId(), "check@companya.com")).isTrue();
    }

    @Test
    void existsByCompanyIdAndEmail_differentCompany_returnsFalse() {
        userRepository.save(buildUser("check@companya.com", companyA.getId()));

        assertThat(userRepository.existsByCompanyIdAndEmail(companyB.getId(), "check@companya.com")).isFalse();
    }

    // ── findByIdAndCompanyId ──────────────────────────────────────────────────

    @Test
    void findByIdAndCompanyId_correctCompany_returnsUser() {
        User saved = userRepository.save(buildUser("scoped@companya.com", companyA.getId()));

        Optional<User> result = userRepository.findByIdAndCompanyId(saved.getId(), companyA.getId());

        assertThat(result).isPresent();
    }

    @Test
    void findByIdAndCompanyId_wrongCompany_returnsEmpty() {
        User saved = userRepository.save(buildUser("scoped@companya.com", companyA.getId()));

        Optional<User> result = userRepository.findByIdAndCompanyId(saved.getId(), companyB.getId());

        assertThat(result).isEmpty();
    }

    // ── Unique email constraint ───────────────────────────────────────────────

    @Test
    void saveUser_duplicateEmail_throwsDataIntegrityViolation() {
        userRepository.save(buildUser("dup@companya.com", companyA.getId()));

        assertThatThrownBy(() -> userRepository.saveAndFlush(buildUser("dup@companya.com", companyB.getId())))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // ── FK constraint on companyId ────────────────────────────────────────────

    @Test
    void saveUser_nonExistentCompanyId_throwsDataIntegrityViolation() {
        User orphan = buildUser("orphan@test.com", "non-existent-company-id");

        assertThatThrownBy(() -> userRepository.saveAndFlush(orphan))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private User buildUser(String email, String companyId) {
        return User.builder()
                .companyId(companyId)
                .email(email)
                .passwordHash("hashed")
                .role(Role.MANAGER)
                .build();
    }
}
