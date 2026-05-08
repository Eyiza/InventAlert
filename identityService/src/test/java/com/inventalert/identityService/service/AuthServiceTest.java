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
import com.inventalert.identityService.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock CompanyRepository companyRepository;
    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtUtil jwtUtil;
    @Mock CompanyEventProducer eventProducer;
    @Mock ModelMapper modelMapper;

    @InjectMocks AuthServiceImpl authService;


    @Test
    void signup_success_savesCompanyAndUserAndPublishesEvent() {
        SignupRequest req = new SignupRequest();
        req.setCompanyName("Dangote Industries");
        req.setAdminEmail("emeka@dangote.ng");
        req.setPassword("password123");

        when(companyRepository.existsByAdminEmail("emeka@dangote.ng")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(companyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(jwtUtil.generateToken(any())).thenReturn("jwt-token");

        LoginResponse mapped = new LoginResponse();
        mapped.setRole("ADMIN");
        mapped.setCompanyId("some-company-id");
        when(modelMapper.map(any(User.class), eq(LoginResponse.class))).thenReturn(mapped);

        LoginResponse response = authService.signup(req);

        assertThat(response.getToken()).isEqualTo("jwt-token");
        assertThat(response.getRole()).isEqualTo("ADMIN");
        assertThat(response.getCompanyId()).isNotNull();
        assertThat(response.getWarehouseId()).isNull();
        verify(companyRepository).save(any(Company.class));
        verify(userRepository).save(any(User.class));
        verify(eventProducer).publishCompanyCreated(anyString(), eq("Dangote Industries"), eq("emeka@dangote.ng"));
    }

    @Test
    void signup_duplicateEmail_throwsEmailAlreadyExistsException() {
        SignupRequest req = new SignupRequest();
        req.setCompanyName("Konga Ltd");
        req.setAdminEmail("dup@konga.ng");
        req.setPassword("password123");

        when(companyRepository.existsByAdminEmail("dup@konga.ng")).thenReturn(true);

        assertThatThrownBy(() -> authService.signup(req))
                .isInstanceOf(EmailAlreadyExistsException.class);

        verify(companyRepository, never()).save(any());
        verify(userRepository, never()).save(any());
        verify(eventProducer, never()).publishCompanyCreated(any(), any(), any());
    }

    @Test
    void login_success_returnsLoginResponse() {
        User user = buildUser("u-1", "co-1", Role.MANAGER, CompanyStatus.ACTIVE, null);
        Company company = buildCompany("co-1", CompanyStatus.ACTIVE);

        LoginRequest req = new LoginRequest();
        req.setEmail("chukwudi@firstbank.ng");
        req.setPassword("pass");

        when(userRepository.findByEmail("chukwudi@firstbank.ng")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("pass", "hashed")).thenReturn(true);
        when(companyRepository.findById("co-1")).thenReturn(Optional.of(company));
        when(jwtUtil.generateToken(user)).thenReturn("manager-jwt");

        LoginResponse mapped = new LoginResponse();
        mapped.setUserId("u-1");
        mapped.setCompanyId("co-1");
        mapped.setRole("MANAGER");
        when(modelMapper.map(eq(user), eq(LoginResponse.class))).thenReturn(mapped);

        LoginResponse response = authService.login(req);

        assertThat(response.getToken()).isEqualTo("manager-jwt");
        assertThat(response.getUserId()).isEqualTo("u-1");
        assertThat(response.getCompanyId()).isEqualTo("co-1");
        assertThat(response.getRole()).isEqualTo("MANAGER");
    }

    @Test
    void login_userNotFound_throwsInvalidCredentials() {
        LoginRequest req = new LoginRequest();
        req.setEmail("unknown@x.ng");
        req.setPassword("p");

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void login_wrongPassword_throwsInvalidCredentials() {
        User user = buildUser("u-1", "co-1", Role.ADMIN, CompanyStatus.ACTIVE, null);

        LoginRequest req = new LoginRequest();
        req.setEmail("bello@corp.ng");
        req.setPassword("wrong");

        when(userRepository.findByEmail("bello@corp.ng")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void login_suspendedCompany_throwsSuspendedCompanyException() {
        User user = buildUser("u-1", "co-1", Role.ADMIN, CompanyStatus.SUSPENDED, null);
        Company company = buildCompany("co-1", CompanyStatus.SUSPENDED);

        LoginRequest req = new LoginRequest();
        req.setEmail("babangida@suspended.ng");
        req.setPassword("pass");

        when(userRepository.findByEmail("babangida@suspended.ng")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("pass", "hashed")).thenReturn(true);
        when(companyRepository.findById("co-1")).thenReturn(Optional.of(company));

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(SuspendedCompanyException.class);
    }

    @Test
    void superAdminLogin_correctCredentials_returnsTokenWithNoCompanyId() {
        setEnvFields("superadmin@inventalert.ng", "SuperSecure123!", "sa-uuid-001");
        when(jwtUtil.generateSuperAdminToken("sa-uuid-001")).thenReturn("sa-token");

        LoginRequest req = new LoginRequest();
        req.setEmail("superadmin@inventalert.ng");
        req.setPassword("SuperSecure123!");

        LoginResponse response = authService.superAdminLogin(req);

        assertThat(response.getToken()).isEqualTo("sa-token");
        assertThat(response.getCompanyId()).isNull();
        assertThat(response.getRole()).isEqualTo("SUPER_ADMIN");
    }

    @Test
    void superAdminLogin_wrongEmail_throwsInvalidCredentials() {
        setEnvFields("superadmin@inventalert.ng", "SuperSecure123!", "sa-uuid-001");

        LoginRequest req = new LoginRequest();
        req.setEmail("wrong@email.ng");
        req.setPassword("SuperSecure123!");

        assertThatThrownBy(() -> authService.superAdminLogin(req))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void superAdminLogin_wrongPassword_throwsInvalidCredentials() {
        setEnvFields("superadmin@inventalert.ng", "SuperSecure123!", "sa-uuid-001");

        LoginRequest req = new LoginRequest();
        req.setEmail("superadmin@inventalert.ng");
        req.setPassword("WrongPass");

        assertThatThrownBy(() -> authService.superAdminLogin(req))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    private User buildUser(String id, String companyId, Role role, CompanyStatus ignored, String warehouseId) {
        return User.builder()
                .id(id)
                .companyId(companyId)
                .email("user@test.ng")
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
