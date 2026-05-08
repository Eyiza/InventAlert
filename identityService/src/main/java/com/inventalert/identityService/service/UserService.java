package com.inventalert.identityService.service;

import com.inventalert.identityService.dto.request.AssignWarehouseRequest;
import com.inventalert.identityService.dto.request.CreateUserRequest;
import com.inventalert.identityService.dto.request.UpdateRoleRequest;
import com.inventalert.identityService.dto.response.AssignmentResponse;
import com.inventalert.identityService.dto.response.UserResponse;
import com.inventalert.identityService.exception.EmailAlreadyExistsException;
import com.inventalert.identityService.exception.UserAlreadyDeactivatedException;
import com.inventalert.identityService.exception.UserNotFoundException;
import com.inventalert.identityService.model.User;
import com.inventalert.identityService.model.WarehouseAssignment;
import com.inventalert.identityService.repository.UserRepository;
import com.inventalert.identityService.repository.WarehouseAssignmentRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final WarehouseAssignmentRepository assignmentRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
                       WarehouseAssignmentRepository assignmentRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.assignmentRepository = assignmentRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public UserResponse createUser(String companyId, CreateUserRequest request) {
        if (userRepository.existsByCompanyIdAndEmail(companyId, request.email())) {
            throw new EmailAlreadyExistsException(request.email());
        }
        User user = User.builder()
                .id(UUID.randomUUID().toString())
                .companyId(companyId)
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(request.role())
                .isActive(true)
                .build();
        return UserResponse.from(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public List<UserResponse> listUsers(String companyId) {
        return userRepository.findAllByCompanyId(companyId)
                .stream().map(UserResponse::from).toList();
    }

    public UserResponse updateRole(String companyId, String userId, UpdateRoleRequest request) {
        User user = userRepository.findByIdAndCompanyId(userId, companyId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        user.setRole(request.role());
        return UserResponse.from(userRepository.save(user));
    }

    public UserResponse deactivateUser(String companyId, String userId) {
        User user = userRepository.findByIdAndCompanyId(userId, companyId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        if (!user.isActive()) throw new UserAlreadyDeactivatedException(userId);
        user.setActive(false);
        return UserResponse.from(userRepository.save(user));
    }

    public AssignmentResponse assignToWarehouse(String companyId, String userId, AssignWarehouseRequest request) {
        userRepository.findByIdAndCompanyId(userId, companyId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        if (assignmentRepository.existsByUserIdAndWarehouseId(userId, request.warehouseId())) {
            return AssignmentResponse.from(
                    assignmentRepository.findAllByUserId(userId).stream()
                            .filter(a -> a.getWarehouseId().equals(request.warehouseId()))
                            .findFirst().orElseThrow()
            );
        }

        WarehouseAssignment assignment = new WarehouseAssignment();
        assignment.setId(UUID.randomUUID().toString());
        assignment.setUserId(userId);
        assignment.setCompanyId(companyId);
        assignment.setWarehouseId(request.warehouseId());
        return AssignmentResponse.from(assignmentRepository.save(assignment));
    }

    @Transactional(readOnly = true)
    public List<AssignmentResponse> getAssignments(String companyId, String userId) {
        userRepository.findByIdAndCompanyId(userId, companyId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        return assignmentRepository.findAllByUserId(userId)
                .stream().map(AssignmentResponse::from).toList();
    }
}
