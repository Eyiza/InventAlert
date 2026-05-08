package com.inventalert.identityService.service;

import com.inventalert.identityService.dto.request.LoginRequest;
import com.inventalert.identityService.dto.request.SignupRequest;
import com.inventalert.identityService.dto.response.LoginResponse;

public interface AuthService {
    LoginResponse signup(SignupRequest request);
    LoginResponse login(LoginRequest request);
    LoginResponse superAdminLogin(LoginRequest request);
}
