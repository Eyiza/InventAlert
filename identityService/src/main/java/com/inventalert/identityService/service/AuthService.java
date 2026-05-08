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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class AuthService {

    private final CompanyRepository   companyRepository;
    private final UserRepository      userRepository;
    private final PasswordEncoder     passwordEncoder;
    private final JwtUtil             jwtUtil;
    private final CompanyEventProducer eventProducer;

    // SuperAdmin credentials loaded from environment / env.properties
    @Value("${SUPER_ADMIN_EMAIL:superadmin@inventalert.io}")
    private String superAdminEmail;

    @Value("${SUPER_ADMIN_PASSWORD:SuperSecure123!}")
    private String superAdminPassword;

    @Value("${SUPER_ADMIN_ID:superadmin-fixed-uuid-0001}")
    private String superAdminId;

    public AuthService(CompanyRepository companyRepository,
                       UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil,
                       CompanyEventProducer eventProducer) {
        this.companyRepository = companyRepository;
        this.userRepository    = userRepository;
        this.passwordEncoder   = passwordEncoder;
        this.jwtUtil           = jwtUtil;
        this.eventProducer     = eventProducer;
    }

    // ── POST /api/auth/signup ─────────────────────────────────────────

    public LoginResponse signup(SignupRequest request) {
        if (companyRepository.existsByAdminEmail(request.adminEmail())) {
            throw new EmailAlreadyExistsException(request.adminEmail());
        }

        String companyId = UUID.randomUUID().toString();
        String adminId   = UUID.randomUUID().toString();

        Company company = Company.builder()
                .id(companyId)
                .companyName(request.companyName())
                .adminEmail(request.adminEmail())
                .build();
        companyRepository.save(company);

        User admin = User.builder()
                .id(adminId)
                .companyId(companyId)
                .email(request.adminEmail())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(Role.ADMIN)
                .build();
        userRepository.save(admin);

        eventProducer.publishCompanyCreated(companyId, request.companyName(), request.adminEmail());

        String token = jwtUtil.generateToken(admin);
        return new LoginResponse(token, adminId, companyId, Role.ADMIN.name(), null);
    }

    // ── POST /api/auth/login ──────────────────────────────────────────

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(InvalidCredentialsException::new);

        if (!user.isActive()) {
            throw new InvalidCredentialsException();
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        Company company = companyRepository.findById(user.getCompanyId())
                .orElseThrow(InvalidCredentialsException::new);
        if (company.getStatus() == CompanyStatus.SUSPENDED) {
            throw new SuspendedCompanyException();
        }

        String token = jwtUtil.generateToken(user);
        return new LoginResponse(
                token, user.getId(), user.getCompanyId(),
                user.getRole().name(), user.getWarehouseId()
        );
    }

    // ── POST /api/auth/superadmin/login ───────────────────────────────

    @Transactional(readOnly = true)
    public LoginResponse superAdminLogin(LoginRequest request) {
        if (!request.email().equals(superAdminEmail)) {
            throw new InvalidCredentialsException();
        }
        // passwordEncoder.matches(raw, encode(stored)) works correctly because
        // BCrypt embeds the salt in the hash — same raw password always matches.
        if (!passwordEncoder.matches(request.password(), passwordEncoder.encode(superAdminPassword))) {
            throw new InvalidCredentialsException();
        }

        String token = jwtUtil.generateSuperAdminToken(superAdminId);
        return new LoginResponse(token, superAdminId, null, "SUPER_ADMIN", null);
    }
}
