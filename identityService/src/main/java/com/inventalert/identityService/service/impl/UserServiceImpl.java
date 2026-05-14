package com.inventalert.identityService.service.impl;

import com.inventalert.identityService.dto.request.AssignWarehouseRequest;
import com.inventalert.identityService.dto.request.CreateUserRequest;
import com.inventalert.identityService.dto.request.UpdateRoleRequest;
import com.inventalert.identityService.dto.response.AssignmentResponse;
import com.inventalert.identityService.dto.response.UserResponse;
import com.inventalert.identityService.exception.AssignmentNotFoundException;
import com.inventalert.identityService.exception.EmailAlreadyExistsException;
import com.inventalert.identityService.exception.UserAlreadyActiveException;
import com.inventalert.identityService.exception.UserAlreadyDeactivatedException;
import com.inventalert.identityService.exception.UserNotFoundException;
import com.inventalert.identityService.exception.WarehouseManagerConflictException;
import com.inventalert.identityService.exception.WarehouseRequiredException;
import com.inventalert.identityService.model.Role;
import com.inventalert.identityService.model.User;
import com.inventalert.identityService.model.WarehouseAssignment;
import com.inventalert.identityService.repository.UserRepository;
import com.inventalert.identityService.repository.WarehouseAssignmentRepository;
import com.inventalert.identityService.service.UserService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.List;

@Service
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final WarehouseAssignmentRepository assignmentRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository,
                           WarehouseAssignmentRepository assignmentRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.assignmentRepository = assignmentRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserResponse createUser(String companyId, CreateUserRequest request, String callerRole) {
        if ("MANAGER".equals(callerRole) && request.role() == Role.ADMIN) {
            throw new AccessDeniedException("Managers cannot create admin users");
        }

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
                .name(request.name())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(request.role())
                .isActive(true)
                .mustChangePassword(true)
                .build();
        user = userRepository.saveAndFlush(user);

        if (request.role() != Role.ADMIN) {
            WarehouseAssignment assignment = new WarehouseAssignment();
            assignment.setUserId(user.getId());
            assignment.setCompanyId(companyId);
            assignment.setWarehouseId(request.warehouseId());
            assignmentRepository.save(assignment);
        }

        return UserResponse.from(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> listUsers(String companyId) {
        return userRepository.findAllByCompanyId(companyId)
                .stream()
                .sorted(Comparator.comparingInt((User u) -> roleOrder(u.getRole()))
                        .thenComparing(User::getEmail))
                .map(UserResponse::from)
                .toList();
    }

    private static int roleOrder(Role role) {
        return switch (role) {
            case ADMIN -> 0;
            case MANAGER -> 1;
            case PROCUREMENT_OFFICER -> 2;
            case WAREHOUSE_STAFF -> 3;
        };
    }

    @Override
    public UserResponse updateRole(String companyId, String userId, UpdateRoleRequest request, String callerRole) {
        if ("MANAGER".equals(callerRole) && request.role() == Role.ADMIN) {
            throw new AccessDeniedException("Managers cannot assign the admin role");
        }

        User user = userRepository.findByIdAndCompanyId(userId, companyId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        if (request.role() == Role.MANAGER) {
            assignmentRepository.findAllByUserId(userId)
                    .forEach(a -> assertNoManagerForWarehouse(a.getWarehouseId(), companyId, userId));
        }

        user.setRole(request.role());
        return UserResponse.from(userRepository.save(user));
    }

    @Override
    public UserResponse deactivateUser(String companyId, String userId, String callerRole) {
        User user = userRepository.findByIdAndCompanyId(userId, companyId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        if ("MANAGER".equals(callerRole) && user.getRole() == Role.ADMIN) {
            throw new AccessDeniedException("Managers cannot deactivate admin users");
        }
        if (!user.isActive()) throw new UserAlreadyDeactivatedException(userId);
        user.setActive(false);
        return UserResponse.from(userRepository.save(user));
    }

    @Override
    public UserResponse reactivateUser(String companyId, String userId, String callerRole) {
        User user = userRepository.findByIdAndCompanyId(userId, companyId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        if ("MANAGER".equals(callerRole) && user.getRole() == Role.ADMIN) {
            throw new AccessDeniedException("Managers cannot reactivate admin users");
        }
        if (user.isActive()) throw new UserAlreadyActiveException(userId);
        user.setActive(true);
        return UserResponse.from(userRepository.save(user));
    }

    @Override
    public AssignmentResponse assignToWarehouse(String companyId, String userId, AssignWarehouseRequest request) {
        User user = userRepository.findByIdAndCompanyId(userId, companyId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        if (user.getRole() == Role.MANAGER) {
            assertNoManagerForWarehouse(request.warehouseId(), companyId, userId);
        }

        // One warehouse per non-admin user — replace any existing assignment
        assignmentRepository.deleteAllByUserId(userId);

        WarehouseAssignment assignment = new WarehouseAssignment();
        assignment.setUserId(userId);
        assignment.setCompanyId(companyId);
        assignment.setWarehouseId(request.warehouseId());
        return AssignmentResponse.from(assignmentRepository.save(assignment));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssignmentResponse> getAssignments(String companyId, String userId) {
        userRepository.findByIdAndCompanyId(userId, companyId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        return assignmentRepository.findAllByUserId(userId)
                .stream().map(AssignmentResponse::from).toList();
    }

    @Override
    public void removeAssignment(String companyId, String userId, String assignmentId) {
        userRepository.findByIdAndCompanyId(userId, companyId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        WarehouseAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new AssignmentNotFoundException(assignmentId));
        if (!assignment.getUserId().equals(userId)) {
            throw new AssignmentNotFoundException(assignmentId);
        }
        assignmentRepository.delete(assignment);
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
