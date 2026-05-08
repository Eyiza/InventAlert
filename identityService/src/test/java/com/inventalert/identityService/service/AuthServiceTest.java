package com.inventalert.identityService.service;

import com.inventalert.identityService.dto.request.LoginRequest;
import com.inventalert.identityService.dto.request.SignupRequest;
import com.inventalert.identityService.dto.response.LoginResponse;
import com.inventalert.identityService.exception.EmailAlreadyExistsException;
import com.inventalert.identityService.kafka.CompanyEventProducer;
import com.inventalert.identityService.model.Company;
import com.inventalert.identityService.model.CompanyStatus;
import com.inventalert.identityService.model.Role;
import com.inventalert.identityService.model.User;
import com.inventalert.identityService.repository.CompanyRepository;
import com.inventalert.identityService.repository.UserRepository;
import com.inventalert.identityService.security.exception.InvalidCredentialsException;
import com.inventalert.identityService.security.exception.SuspendedCompanyException;
import com.inventalert.identityService.security.service.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock CompanyRepository   companyRepository;
    @Mock UserRepository      userRepository;
    @Mock PasswordEncoder     passwordEncoder;
    @Mock JwtUtil             jwtUtil;
    @Mock CompanyEventProducer eventProducer;

    @InjectMocks AuthService authService;

    // ── signup ──────────────────────────────────────────────────────────

    @Test
    void signup_success_savesCompanyAndUserAndPublishesEvent() {
        SignupRequest req = new SignupRequest("Acme Corp", "admin@acme.io", "password123");
        when(companyRepository.existsByAdminEmail("admin@acme.io")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(companyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(jwtUtil.generateToken(any())).thenReturn("jwt-token");

        LoginResponse response = authService.signup(req);

        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.role()).isEqualTo("ADMIN");
        assertThat(response.companyId()).isNotNull();
        assertThat(response.warehouseId()).isNull();
        verify(companyRepository).save(any(Company.class));
        verify(userRepository).save(any(User.class));
        verify(eventProducer).publishCompanyCreated(anyString(), eq("Acme Corp"), eq("admin@acme.io"));
    }

    @Test
    void signup_duplicateEmail_throwsEmailAlreadyExistsException() {
        SignupRequest req = new SignupRequest("Dupe Corp", "dup@test.io", "password123");
        when(companyRepository.existsByAdminEmail("dup@test.io")).thenReturn(true);

        assertThatThrownBy(() -> authService.signup(req))
                .isInstanceOf(EmailAlreadyExistsException.class);

        verify(companyRepository, never()).save(any());
        verify(userRepository, never()).save(any());
        verify(eventProducer, never()).publishCompanyCreated(any(), any(), any());
    }

    // ── login ───────────────────────────────────────────────────────────

    @Test
    void login_success_returnsLoginResponse() {
        User user = buildUser("u-1", "co-1", Role.MANAGER, CompanyStatus.ACTIVE, null);
        Company company = buildCompany("co-1", CompanyStatus.ACTIVE);

        LoginRequest req = new LoginRequest("mgr@corp.io", "pass");
        when(userRepository.findByEmail("mgr@corp.io")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("pass", "hashed")).thenReturn(true);
        when(companyRepository.findById("co-1")).thenReturn(Optional.of(company));
        when(jwtUtil.generateToken(user)).thenReturn("manager-jwt");

        LoginResponse response = authService.login(req);

        assertThat(response.token()).isEqualTo("manager-jwt");
        assertThat(response.userId()).isEqualTo("u-1");
        assertThat(response.companyId()).isEqualTo("co-1");
        assertThat(response.role()).isEqualTo("MANAGER");
    }

    @Test
    void login_userNotFound_throwsInvalidCredentials() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("x@x.io", "p")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void login_wrongPassword_throwsInvalidCredentials() {
        User user = buildUser("u-1", "co-1", Role.ADMIN, CompanyStatus.ACTIVE, null);
        when(userRepository.findByEmail("admin@corp.io")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("admin@corp.io", "wrong")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void login_suspendedCompany_throwsSuspendedCompanyException() {
        User user = buildUser("u-1", "co-1", Role.ADMIN, CompanyStatus.SUSPENDED, null);
        Company company = buildCompany("co-1", CompanyStatus.SUSPENDED);

        when(userRepository.findByEmail("admin@susp.io")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("pass", "hashed")).thenReturn(true);
        when(companyRepository.findById("co-1")).thenReturn(Optional.of(company));

        assertThatThrownBy(() -> authService.login(new LoginRequest("admin@susp.io", "pass")))
                .isInstanceOf(SuspendedCompanyException.class);
    }

    // ── superAdminLogin ─────────────────────────────────────────────────

    @Test
    void superAdminLogin_correctCredentials_returnsTokenWithNoCompanyId() {
        setEnvFields("superadmin@inventalert.io", "SuperSecure123!", "sa-uuid-001");
        when(passwordEncoder.matches(eq("SuperSecure123!"), anyString())).thenReturn(true);
        when(jwtUtil.generateSuperAdminToken("sa-uuid-001")).thenReturn("sa-token");

        LoginResponse response = authService.superAdminLogin(
                new LoginRequest("superadmin@inventalert.io", "SuperSecure123!"));

        assertThat(response.token()).isEqualTo("sa-token");
        assertThat(response.companyId()).isNull();
        assertThat(response.role()).isEqualTo("SUPER_ADMIN");
    }

    @Test
    void superAdminLogin_wrongEmail_throwsInvalidCredentials() {
        setEnvFields("superadmin@inventalert.io", "SuperSecure123!", "sa-uuid-001");

        assertThatThrownBy(() -> authService.superAdminLogin(
                new LoginRequest("wrong@email.io", "SuperSecure123!")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void superAdminLogin_wrongPassword_throwsInvalidCredentials() {
        setEnvFields("superadmin@inventalert.io", "SuperSecure123!", "sa-uuid-001");
        when(passwordEncoder.matches(eq("WrongPass"), anyString())).thenReturn(false);

        assertThatThrownBy(() -> authService.superAdminLogin(
                new LoginRequest("superadmin@inventalert.io", "WrongPass")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    // ── helpers ─────────────────────────────────────────────────────────

    private User buildUser(String id, String companyId, Role role, CompanyStatus ignored, String warehouseId) {
        return User.builder()
                .id(id)
                .companyId(companyId)
                .email("user@test.io")
                .passwordHash("hashed")
                .role(role)
                .warehouseId(warehouseId)
                .build();
    }

    private Company buildCompany(String id, CompanyStatus status) {
        Company c = new Company();
        c.setId(id);
        c.setStatus(status);
        return c;
    }

    private void setEnvFields(String email, String password, String id) {
        ReflectionTestUtils.setField(authService, "superAdminEmail", email);
        ReflectionTestUtils.setField(authService, "superAdminPassword", password);
        ReflectionTestUtils.setField(authService, "superAdminId", id);
    }
}
