package com.tms.as.service;

import com.tms.as.dto.*;

public interface AuthService {

    UserResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    UserResponse getProfile(String id, String loggedInEmail);

    UserResponse updateProfile(String id, UpdateProfileRequest request, String loggedInEmail);

    UserResponse adminUpdateUser(String id, AdminUpdateUserRequest request);

    UserResponse assignManager(String id, String managerId);
    
    String getManagerForEmployee(String employeeId);
}
