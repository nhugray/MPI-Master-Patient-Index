package com.mpi.demo.service;

import com.mpi.demo.dto.request.ChangePasswordRequest;
import com.mpi.demo.dto.request.LoginRequest;
import com.mpi.demo.dto.request.RegisterRequest;
import com.mpi.demo.dto.response.LoginResponse;
import com.mpi.demo.dto.response.RegisterResponse;
import com.mpi.demo.dto.response.UserResponse;

public interface AuthService {
    LoginResponse login(LoginRequest request, String deviceInfo, String ipAddress);

    RegisterResponse register(RegisterRequest request);

    LoginResponse refresh(String rawRefreshToken, String currentIP);

    void logout(String rawRefreshToken);

    UserResponse getMe(String email);

    void changePassword(String email, ChangePasswordRequest request);
}
