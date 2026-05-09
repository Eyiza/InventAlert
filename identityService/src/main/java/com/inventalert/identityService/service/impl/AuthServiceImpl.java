package com.inventalert.identityService.service.impl;

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

@Service
@Transactional
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final WarehouseAssignmentRepository assignmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final CompanyEventProducer eventProducer;

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

        return buildLoginResponse(savedAdmin);
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

        return buildLoginResponse(user);
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

    private LoginResponse buildLoginResponse(User user) {
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
        response.setCompanyId(user.getCompanyId());
        response.setRole(user.getRole().name());
        response.setWarehouseId(warehouseId);
        return response;
    }
}
