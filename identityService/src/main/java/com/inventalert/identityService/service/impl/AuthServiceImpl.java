package com.inventalert.identityService.service.impl;

import com.inventalert.identityService.dto.request.ForgotPasswordRequest;
import com.inventalert.identityService.dto.request.LoginRequest;
import com.inventalert.identityService.dto.request.ResetPasswordRequest;
import com.inventalert.identityService.dto.request.SignupRequest;
import com.inventalert.identityService.dto.response.LoginResponse;
import com.inventalert.identityService.exception.EmailAlreadyExistsException;
import com.inventalert.identityService.exception.InvalidResetTokenException;
import com.inventalert.identityService.kafka.CompanyEventProducer;
import com.inventalert.identityService.kafka.PasswordResetEventProducer;
import com.inventalert.identityService.model.Company;
import com.inventalert.identityService.model.CompanyStatus;
import com.inventalert.identityService.model.PasswordResetToken;
import com.inventalert.identityService.model.Role;
import com.inventalert.identityService.model.User;
import com.inventalert.identityService.repository.CompanyRepository;
import com.inventalert.identityService.repository.PasswordResetTokenRepository;
import com.inventalert.identityService.repository.UserRepository;
import com.inventalert.identityService.repository.WarehouseAssignmentRepository;
import com.inventalert.identityService.security.exception.InvalidCredentialsException;
import com.inventalert.identityService.security.exception.SuspendedCompanyException;
import com.inventalert.identityService.security.service.JwtUtil;
import com.inventalert.identityService.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final WarehouseAssignmentRepository assignmentRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final CompanyEventProducer eventProducer;
    private final PasswordResetEventProducer passwordResetEventProducer;

    @Value("${SUPER_ADMIN_EMAIL:superadmin@inventalert.io}")
    private String superAdminEmail;

    @Value("${SUPER_ADMIN_PASSWORD:SuperSecure123!}")
    private String superAdminPassword;

    @Value("${SUPER_ADMIN_ID:superadmin-fixed-uuid-0001}")
    private String superAdminId;

    @Override
    public LoginResponse signup(SignupRequest request) {
        if (companyRepository.existsByAdminEmail(request.getAdminEmail())) {
            throw new EmailAlreadyExistsException(request.getAdminEmail());
        }

        Company company = Company.builder()
                .companyName(request.getCompanyName())
                .adminEmail(request.getAdminEmail())
                .logoUrl(request.getLogoUrl())
                .build();
        Company savedCompany = companyRepository.save(company);

        User admin = User.builder()
                .companyId(savedCompany.getId())
                .email(request.getAdminEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(Role.ADMIN)
                .build();
        User savedAdmin = userRepository.save(admin);

        eventProducer.publishCompanyCreated(savedCompany.getId(), request.getCompanyName(), request.getAdminEmail());

        return buildLoginResponse(savedAdmin, savedCompany);
    }

    @Override
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(InvalidCredentialsException::new);

        if (!user.isActive()) {
            throw new InvalidCredentialsException();
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        Company company = companyRepository.findById(user.getCompanyId())
                .orElseThrow(InvalidCredentialsException::new);
        if (company.getStatus() == CompanyStatus.SUSPENDED) {
            throw new SuspendedCompanyException();
        }

        return buildLoginResponse(user, company);
    }

    @Override
    @Transactional(readOnly = true)
    public LoginResponse superAdminLogin(LoginRequest request) {
        if (!superAdminEmail.equals(request.getEmail()) || !superAdminPassword.equals(request.getPassword())) {
            throw new InvalidCredentialsException();
        }

        LoginResponse response = new LoginResponse();
        response.setToken(jwtUtil.generateSuperAdminToken(superAdminId));
        response.setUserId(superAdminId);
        response.setRole("SUPER_ADMIN");
        return response;
    }

    @Override
    public void forgotPassword(ForgotPasswordRequest request) {
        userRepository.findByEmail(request.email()).filter(User::isActive).ifPresent(user -> {
            // Invalidate any existing pending token for this user before issuing a new one
            passwordResetTokenRepository.findByUserIdAndUsedFalse(user.getId())
                    .ifPresent(existing -> {
                        existing.setUsed(true);
                        passwordResetTokenRepository.save(existing);
                    });

            String token = UUID.randomUUID().toString();
            LocalDateTime expiresAt = LocalDateTime.now().plusHours(1);

            PasswordResetToken resetToken = PasswordResetToken.builder()
                    .userId(user.getId())
                    .token(token)
                    .expiresAt(expiresAt)
                    .build();
            passwordResetTokenRepository.save(resetToken);

            passwordResetEventProducer.publishPasswordResetRequested(
                    user.getId(), user.getEmail(), token, expiresAt);
        });
        // Always returns normally — never reveal whether the email is registered
    }

    @Override
    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(request.token())
                .orElseThrow(InvalidResetTokenException::new);

        if (resetToken.isUsed()) throw new InvalidResetTokenException();
        if (resetToken.getExpiresAt().isBefore(LocalDateTime.now())) throw new InvalidResetTokenException();

        User user = userRepository.findById(resetToken.getUserId())
                .orElseThrow(InvalidResetTokenException::new);

        if (!user.isActive()) throw new InvalidResetTokenException();

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setMustChangePassword(false);
        userRepository.save(user);

        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);
    }

    private LoginResponse buildLoginResponse(User user, Company company) {
        // ADMIN is company-scoped; warehouse roles carry their primary warehouse in the token
        String warehouseId = null;
        if (user.getRole() != Role.ADMIN) {
            warehouseId = assignmentRepository.findAllByUserId(user.getId())
                    .stream()
                    .findFirst()
                    .map(a -> a.getWarehouseId())
                    .orElse(null);
        }

        LoginResponse response = new LoginResponse();
        response.setToken(jwtUtil.generateToken(user, warehouseId));
        response.setUserId(user.getId());
        response.setEmail(user.getEmail());
        response.setCompanyId(user.getCompanyId());
        response.setCompanyName(company.getCompanyName());
        response.setCompanyLogo(company.getLogoUrl());
        response.setRole(user.getRole().name());
        response.setWarehouseId(warehouseId);
        response.setMustChangePassword(user.isMustChangePassword());
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public LoginResponse refreshToken(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Company company = companyRepository.findById(user.getCompanyId())
                .orElseThrow(() -> new RuntimeException("Company not found"));
        return buildLoginResponse(user, company);
    }

    @Override
    public void changePassword(String userId, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setMustChangePassword(false);
        userRepository.save(user);
    }
}
