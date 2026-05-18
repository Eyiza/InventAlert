package com.inventalert.identityService.service;

import com.inventalert.identityService.dto.request.CreateUserRequest;
import com.inventalert.identityService.dto.response.UserResponse;
import com.inventalert.identityService.model.Role;
import com.inventalert.identityService.model.User;
import com.inventalert.identityService.repository.UserRepository;
import com.inventalert.identityService.repository.WarehouseAssignmentRepository;
import com.inventalert.identityService.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.inventalert.identityService.dto.request.AssignWarehouseRequest;
import com.inventalert.identityService.dto.request.UpdateRoleRequest;
import com.inventalert.identityService.dto.response.AssignmentResponse;
import com.inventalert.identityService.model.WarehouseAssignment;
import com.inventalert.identityService.exception.EmailAlreadyExistsException;
import com.inventalert.identityService.exception.UserAlreadyDeactivatedException;
import com.inventalert.identityService.exception.UserNotFoundException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private WarehouseAssignmentRepository assignmentRepository;
    @Mock private PasswordEncoder passwordEncoder;

    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(userRepository, assignmentRepository, passwordEncoder);
    }

    @Test
    void CreateUserAccount_CheckIfSuccessfulTest() {
        String companyId = "company-1";
        CreateUserRequest request = new CreateUserRequest("Bob", "bob@acme.com", "password123", Role.MANAGER, "warehouse-1");

        when(userRepository.existsByCompanyIdAndEmail(companyId, request.email())).thenReturn(false);
        when(assignmentRepository.findAllByWarehouseIdAndCompanyId("warehouse-1", companyId)).thenReturn(List.of());
        when(passwordEncoder.encode(request.password())).thenReturn("hashed-password");

        User savedUser = User.builder()
                .id("generated-id")
                .companyId(companyId)
                .email(request.email())
                .passwordHash("hashed-password")
                .role(request.role())
                .isActive(true)
                .build();
        when(userRepository.saveAndFlush(any(User.class))).thenReturn(savedUser);

        UserResponse response = userService.createUser(companyId, request, "ADMIN");

        assertThat(response.email()).isEqualTo("bob@acme.com");
        assertThat(response.companyId()).isEqualTo(companyId);
        assertThat(response.role()).isEqualTo(Role.MANAGER);
        assertThat(response.isActive()).isTrue();
        verify(userRepository).saveAndFlush(any(User.class));
        verify(assignmentRepository).save(any(WarehouseAssignment.class));
    }

    @Test
    void CreateUserAccount_DuplicateEmail_CheckIfThrowsExceptionTest() {
        String companyId = "company-1";
        CreateUserRequest request = new CreateUserRequest("Bob", "bob@acme.com", "password123", Role.MANAGER, null);

        when(userRepository.existsByCompanyIdAndEmail(companyId, request.email())).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(companyId, request, "ADMIN"))
                .isInstanceOf(EmailAlreadyExistsException.class)
                .hasMessageContaining("bob@acme.com");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void ListUsers_CheckIfReturnsAllTest() {
        String companyId = "company-1";

        List<User> users = List.of(
                User.builder().id("u1").companyId(companyId).email("a@acme.com").role(Role.MANAGER).isActive(true).passwordHash("h").build(),
                User.builder().id("u2").companyId(companyId).email("b@acme.com").role(Role.ADMIN).isActive(true).passwordHash("h").build(),
                User.builder().id("u3").companyId(companyId).email("c@acme.com").role(Role.WAREHOUSE_STAFF).isActive(false).passwordHash("h").build()
        );
        when(userRepository.findAllByCompanyId(companyId)).thenReturn(users);

        List<UserResponse> result = userService.listUsers(companyId);

        assertThat(result).hasSize(3);
        assertThat(result).extracting(UserResponse::email)
                .containsExactly("b@acme.com", "a@acme.com", "c@acme.com");
    }

    @Test
    void UpdateRole_CheckIfSuccessfulTest() {
        String companyId = "company-1";
        String userId = "user-1";
        UpdateRoleRequest request = new UpdateRoleRequest(Role.WAREHOUSE_STAFF);

        User existingUser = User.builder()
                .id(userId)
                .companyId(companyId)
                .email("bob@acme.com")
                .passwordHash("h")
                .role(Role.MANAGER)
                .isActive(true)
                .build();

        User updatedUser = User.builder()
                .id(userId)
                .companyId(companyId)
                .email("bob@acme.com")
                .passwordHash("h")
                .role(Role.WAREHOUSE_STAFF)
                .isActive(true)
                .build();

        when(userRepository.findByIdAndCompanyId(userId, companyId)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(existingUser)).thenReturn(updatedUser);

        UserResponse response = userService.updateRole(companyId, userId, request, "ADMIN");

        assertThat(response.role()).isEqualTo(Role.WAREHOUSE_STAFF);
        assertThat(response.id()).isEqualTo(userId);
        verify(userRepository).save(existingUser);
    }

    @Test
    void UpdateRole_UserNotFound_CheckIfThrowsExceptionTest() {
        String companyId = "company-1";
        String userId = "non-existent-user";
        UpdateRoleRequest request = new UpdateRoleRequest(Role.MANAGER);

        when(userRepository.findByIdAndCompanyId(userId, companyId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateRole(companyId, userId, request, "ADMIN"))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining(userId);

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void DeactivateUser_CheckIfSuccessfulTest() {
        String companyId = "company-1";
        String userId = "user-1";

        User activeUser = User.builder()
                .id(userId)
                .companyId(companyId)
                .email("bob@acme.com")
                .passwordHash("h")
                .role(Role.MANAGER)
                .isActive(true)
                .build();

        User deactivatedUser = User.builder()
                .id(userId)
                .companyId(companyId)
                .email("bob@acme.com")
                .passwordHash("h")
                .role(Role.MANAGER)
                .isActive(false)
                .build();

        when(userRepository.findByIdAndCompanyId(userId, companyId)).thenReturn(Optional.of(activeUser));
        when(userRepository.save(activeUser)).thenReturn(deactivatedUser);

        UserResponse response = userService.deactivateUser(companyId, userId, "ADMIN");

        assertThat(response.isActive()).isFalse();
        assertThat(response.id()).isEqualTo(userId);
        verify(userRepository).save(activeUser);
    }

    @Test
    void DeactivateUser_AlreadyInactive_CheckIfThrowsExceptionTest() {
        String companyId = "company-1";
        String userId = "user-1";

        User inactiveUser = User.builder()
                .id(userId)
                .companyId(companyId)
                .email("bob@acme.com")
                .passwordHash("h")
                .role(Role.MANAGER)
                .isActive(false)
                .build();

        when(userRepository.findByIdAndCompanyId(userId, companyId)).thenReturn(Optional.of(inactiveUser));

        assertThatThrownBy(() -> userService.deactivateUser(companyId, userId, "ADMIN"))
                .isInstanceOf(UserAlreadyDeactivatedException.class)
                .hasMessageContaining(userId);

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void AssignToWarehouse_CheckIfSuccessfulTest() {
        String companyId = "company-1";
        String userId = "user-1";
        String warehouseId = "warehouse-1";
        AssignWarehouseRequest request = new AssignWarehouseRequest(warehouseId);

        User user = User.builder()
                .id(userId)
                .companyId(companyId)
                .email("bob@acme.com")
                .passwordHash("h")
                .role(Role.WAREHOUSE_STAFF)
                .isActive(true)
                .build();

        WarehouseAssignment savedAssignment = new WarehouseAssignment();
        savedAssignment.setId("assignment-1");
        savedAssignment.setUserId(userId);
        savedAssignment.setCompanyId(companyId);
        savedAssignment.setWarehouseId(warehouseId);

        when(userRepository.findByIdAndCompanyId(userId, companyId)).thenReturn(Optional.of(user));
        when(assignmentRepository.save(any(WarehouseAssignment.class))).thenReturn(savedAssignment);

        AssignmentResponse response = userService.assignToWarehouse(companyId, userId, request);

        assertThat(response.userId()).isEqualTo(userId);
        assertThat(response.companyId()).isEqualTo(companyId);
        assertThat(response.warehouseId()).isEqualTo(warehouseId);
        verify(assignmentRepository).save(any(WarehouseAssignment.class));
    }

    @Test
    void AssignToWarehouse_AlreadyAssigned_CheckIfReturnsExistingTest() {
        String companyId = "company-1";
        String userId = "user-1";
        String warehouseId = "warehouse-1";
        AssignWarehouseRequest request = new AssignWarehouseRequest(warehouseId);

        User user = User.builder()
                .id(userId)
                .companyId(companyId)
                .email("bob@acme.com")
                .passwordHash("h")
                .role(Role.WAREHOUSE_STAFF)
                .isActive(true)
                .build();

        WarehouseAssignment existingAssignment = new WarehouseAssignment();
        existingAssignment.setId("assignment-1");
        existingAssignment.setUserId(userId);
        existingAssignment.setCompanyId(companyId);
        existingAssignment.setWarehouseId(warehouseId);

        when(userRepository.findByIdAndCompanyId(userId, companyId)).thenReturn(Optional.of(user));
        when(assignmentRepository.save(any(WarehouseAssignment.class))).thenReturn(existingAssignment);

        AssignmentResponse response = userService.assignToWarehouse(companyId, userId, request);

        assertThat(response.id()).isEqualTo("assignment-1");
        assertThat(response.warehouseId()).isEqualTo(warehouseId);
        verify(assignmentRepository).save(any(WarehouseAssignment.class));
    }

    @Test
    void GetAssignments_UserNotFound_CheckIfThrowsExceptionTest() {
        String companyId = "company-1";
        String userId = "non-existent-user";

        when(userRepository.findByIdAndCompanyId(userId, companyId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getAssignments(companyId, userId))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining(userId);

        verify(assignmentRepository, never()).findAllByUserId(any());
    }
}
