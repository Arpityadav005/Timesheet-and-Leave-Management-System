package com.tms.as.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tms.as.dto.*;
import com.tms.as.entity.Role;
import com.tms.as.entity.Status;
import com.tms.as.security.JwtAuthenticationFilter;
import com.tms.as.service.AuthService;
import com.tms.as.service.UserCacheService;
import com.tms.common.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = AuthController.class, properties = {
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false",
        "app.jwt.secret=01234567890123456789012345678901",
        "app.jwt.expiration-ms=3600000"
})
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private AuthController authController;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private UserCacheService userCacheService;

    private UserResponse userResponse;
    private AuthResponse authResponse;
    private Authentication employeeAuthentication;

    @BeforeEach
    void setUp() {
        userResponse = new UserResponse();
        userResponse.setId("USR-ABC12345");
        userResponse.setFullName("John Doe");
        userResponse.setEmail("john@example.com");
        userResponse.setEmployeeCode("EMP-001");
        userResponse.setRole(Role.EMPLOYEE);
        userResponse.setStatus(Status.ACTIVE);
        userResponse.setCreatedAt(LocalDateTime.now());
        userResponse.setUpdatedAt(LocalDateTime.now());

        authResponse = new AuthResponse(
                "jwt-token", "USR-ABC12345", "john@example.com", "John Doe", "EMPLOYEE"
        );
        employeeAuthentication = new UsernamePasswordAuthenticationToken("john@example.com", null);
    }

    @Test
    @DisplayName("POST /register - Should register successfully")
    void register_Success() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("John Doe");
        request.setEmail("john@example.com");
        request.setEmployeeCode("EMP-001");
        request.setPassword("Password1!");

        when(authService.register(any(RegisterRequest.class))).thenReturn(userResponse);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("USR-ABC12345"))
                .andExpect(jsonPath("$.fullName").value("John Doe"))
                .andExpect(jsonPath("$.email").value("john@example.com"))
                .andExpect(jsonPath("$.role").value("EMPLOYEE"));
    }

    @Test
    @DisplayName("POST /login - Should login successfully")
    void login_Success() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("john@example.com");
        request.setPassword("Password1!");

        when(authService.login(any(LoginRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.id").value("USR-ABC12345"))
                .andExpect(jsonPath("$.email").value("john@example.com"))
                .andExpect(jsonPath("$.role").value("EMPLOYEE"));
    }

    @Test
    @DisplayName("GET /profile/{id} - Should return profile")
    void getProfile_Success() {
        when(authService.getProfile(anyString(), anyString())).thenReturn(userResponse);

        ResponseEntity<UserResponse> response =
                authController.getProfile("USR-ABC12345", employeeAuthentication);

        org.junit.jupiter.api.Assertions.assertEquals(200, response.getStatusCode().value());
        org.junit.jupiter.api.Assertions.assertNotNull(response.getBody());
        org.junit.jupiter.api.Assertions.assertEquals("USR-ABC12345", response.getBody().getId());
        org.junit.jupiter.api.Assertions.assertEquals("John Doe", response.getBody().getFullName());
    }

    @Test
    @DisplayName("PUT /profile/{id} - Should update profile")
    void updateProfile_Success() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFullName("John Updated");
        request.setEmail("john@example.com");

        UserResponse updatedResponse = new UserResponse();
        updatedResponse.setId("USR-ABC12345");
        updatedResponse.setFullName("John Updated");
        updatedResponse.setEmail("john@example.com");
        updatedResponse.setEmployeeCode("EMP-001");
        updatedResponse.setRole(Role.EMPLOYEE);
        updatedResponse.setStatus(Status.ACTIVE);

        when(authService.updateProfile(anyString(), any(UpdateProfileRequest.class), anyString()))
                .thenReturn(updatedResponse);

        ResponseEntity<UserResponse> response =
                authController.updateProfile("USR-ABC12345", request, employeeAuthentication);

        org.junit.jupiter.api.Assertions.assertEquals(200, response.getStatusCode().value());
        org.junit.jupiter.api.Assertions.assertNotNull(response.getBody());
        org.junit.jupiter.api.Assertions.assertEquals("John Updated", response.getBody().getFullName());
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
    @DisplayName("PUT /admin/users/{id} - Should admin update user")
    void adminUpdateUser_Success() throws Exception {
        AdminUpdateUserRequest request = new AdminUpdateUserRequest();
        request.setFullName("John Admin Updated");
        request.setEmail("john@example.com");
        request.setEmployeeCode("EMP-001");
        request.setRole(Role.MANAGER);
        request.setStatus(Status.ACTIVE);

        UserResponse updatedResponse = new UserResponse();
        updatedResponse.setId("USR-ABC12345");
        updatedResponse.setFullName("John Admin Updated");
        updatedResponse.setEmail("john@example.com");
        updatedResponse.setRole(Role.MANAGER);
        updatedResponse.setStatus(Status.ACTIVE);

        when(authService.adminUpdateUser(anyString(), any(AdminUpdateUserRequest.class)))
                .thenReturn(updatedResponse);

        mockMvc.perform(put("/api/v1/auth/admin/users/USR-ABC12345")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("John Admin Updated"))
                .andExpect(jsonPath("$.role").value("MANAGER"));
    }

    @Test
    @DisplayName("POST /register - Should return 400 for invalid request")
    void register_InvalidRequest() throws Exception {
        RegisterRequest request = new RegisterRequest();
        // All fields blank — should trigger validation errors

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
