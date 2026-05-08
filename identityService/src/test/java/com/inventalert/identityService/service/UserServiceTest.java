package com.inventalert.identityService.service;

import com.inventalert.identityService.dto.request.CreateUserRequest;
import com.inventalert.identityService.dto.response.UserResponse;
import com.inventalert.identityService.model.Role;
import com.inventalert.identityService.model.User;
import com.inventalert.identityService.repository.UserRepository;
import com.inventalert.identityService.repository.WarehouseAssignmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private WarehouseAssignmentRepository assignmentRepository;
    @Mock private PasswordEncoder passwordEncoder;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, assignmentRepository, passwordEncoder);
    }

    @Test
    void CreateUserAccount_CheckIfSuccessfulTest() {
        String companyId = "company-1";
        CreateUserRequest request = new CreateUserRequest("bob@acme.com", "password123", Role.MANAGER);

        when(userRepository.existsByCompanyIdAndEmail(companyId, request.email())).thenReturn(false);
        when(passwordEncoder.encode(request.password())).thenReturn("hashed-password");

        User savedUser = User.builder()
                .id("generated-id")
                .companyId(companyId)
                .email(request.email())
                .passwordHash("hashed-password")
                .role(request.role())
                .isActive(true)
                .build();
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        UserResponse response = userService.createUser(companyId, request);

        assertThat(response.email()).isEqualTo("bob@acme.com");
        assertThat(response.companyId()).isEqualTo(companyId);
        assertThat(response.role()).isEqualTo(Role.MANAGER);
        assertThat(response.isActive()).isTrue();
        verify(userRepository, times(1)).save(any(User.class));
    }
}
