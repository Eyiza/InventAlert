package com.inventalert.identityService.service;

import com.inventalert.identityService.dto.request.AssignWarehouseRequest;
import com.inventalert.identityService.dto.request.CreateUserRequest;
import com.inventalert.identityService.dto.request.UpdateRoleRequest;
import com.inventalert.identityService.dto.response.AssignmentResponse;
import com.inventalert.identityService.dto.response.UserResponse;
import com.inventalert.identityService.exception.EmailAlreadyExistsException;
import com.inventalert.identityService.exception.UserAlreadyDeactivatedException;
import com.inventalert.identityService.exception.UserNotFoundException;
import com.inventalert.identityService.exception.WarehouseManagerConflictException;
import com.inventalert.identityService.exception.WarehouseRequiredException;
import com.inventalert.identityService.model.Role;
import com.inventalert.identityService.model.User;
import com.inventalert.identityService.model.WarehouseAssignment;
import com.inventalert.identityService.repository.UserRepository;
import com.inventalert.identityService.repository.WarehouseAssignmentRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

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

        if (request.role() != Role.ADMIN) {
            if (!StringUtils.hasText(request.warehouseId())) {
                throw new WarehouseRequiredException(request.role());
            }
            if (request.role() == Role.MANAGER) {
                assertNoManagerForWarehouse(request.warehouseId(), companyId, null);
            }
        }

        User user = User.builder()
                .companyId(companyId)
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(request.role())
                .isActive(true)
                .build();
        user = userRepository.save(user);

        if (request.role() != Role.ADMIN) {
            WarehouseAssignment assignment = new WarehouseAssignment();
            assignment.setUserId(user.getId());
            assignment.setCompanyId(companyId);
            assignment.setWarehouseId(request.warehouseId());
            assignmentRepository.save(assignment);
        }

        return UserResponse.from(user);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> listUsers(String companyId) {
        return userRepository.findAllByCompanyId(companyId)
                .stream().map(UserResponse::from).toList();
    }

    public UserResponse updateRole(String companyId, String userId, UpdateRoleRequest request) {
        User user = userRepository.findByIdAndCompanyId(userId, companyId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        if (request.role() == Role.MANAGER) {
            assignmentRepository.findAllByUserId(userId)
                    .forEach(a -> assertNoManagerForWarehouse(a.getWarehouseId(), companyId, userId));
        }

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
        User user = userRepository.findByIdAndCompanyId(userId, companyId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        if (assignmentRepository.existsByUserIdAndWarehouseId(userId, request.warehouseId())) {
            return AssignmentResponse.from(
                    assignmentRepository.findAllByUserId(userId).stream()
                            .filter(a -> a.getWarehouseId().equals(request.warehouseId()))
                            .findFirst().orElseThrow()
            );
        }

        if (user.getRole() == Role.MANAGER) {
            assertNoManagerForWarehouse(request.warehouseId(), companyId, userId);
        }

        WarehouseAssignment assignment = new WarehouseAssignment();
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

    // Checks that no active manager OTHER than excludeUserId is assigned to this warehouse.
    // Pass null for excludeUserId when creating a new user (no ID to exclude yet).
    private void assertNoManagerForWarehouse(String warehouseId, String companyId, String excludeUserId) {
        boolean conflict = assignmentRepository
                .findAllByWarehouseIdAndCompanyId(warehouseId, companyId)
                .stream()
                .filter(a -> !a.getUserId().equals(excludeUserId))
                .anyMatch(a -> userRepository.findById(a.getUserId())
                        .map(u -> u.getRole() == Role.MANAGER && u.isActive())
                        .orElse(false));
        if (conflict) {
            throw new WarehouseManagerConflictException(warehouseId);
        }
    }
}
