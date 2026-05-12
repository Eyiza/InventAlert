package com.inventalert.identityService.service;

import com.inventalert.identityService.dto.request.AssignWarehouseRequest;
import com.inventalert.identityService.dto.request.CreateUserRequest;
import com.inventalert.identityService.dto.request.UpdateRoleRequest;
import com.inventalert.identityService.dto.response.AssignmentResponse;
import com.inventalert.identityService.dto.response.UserResponse;

import java.util.List;

public interface UserService {
    UserResponse createUser(String companyId, CreateUserRequest request, String callerRole);
    List<UserResponse> listUsers(String companyId);
    UserResponse updateRole(String companyId, String userId, UpdateRoleRequest request, String callerRole);
    UserResponse deactivateUser(String companyId, String userId, String callerRole);
    UserResponse reactivateUser(String companyId, String userId, String callerRole);
    AssignmentResponse assignToWarehouse(String companyId, String userId, AssignWarehouseRequest request);
    List<AssignmentResponse> getAssignments(String companyId, String userId);
    void removeAssignment(String companyId, String userId, String assignmentId);
}
