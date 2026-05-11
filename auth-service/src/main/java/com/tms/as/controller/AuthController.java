package com.tms.as.controller;

import com.tms.as.dto.AdminUpdateUserRequest;
import com.tms.as.dto.AssignManagerRequest;
import com.tms.as.dto.AuthResponse;
import com.tms.as.dto.LoginRequest;
import com.tms.as.dto.RegisterRequest;
import com.tms.as.dto.UpdateProfileRequest;
import com.tms.as.dto.UserResponse;
import com.tms.as.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth", description = "Authentication endpoints")
public class AuthController {
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new employee")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Registering user with employeeCode={} email={}", request.getEmployeeCode(), request.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    @Operation(summary = "Login and get JWT token")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("Login requested for email={}", request.getEmail());
        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping("/profile/{id}")
    @Operation(summary = "Get user profile", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<UserResponse> getProfile(@PathVariable String id,
                                                    Authentication authentication) {
        log.info("Fetching profile for userId={} requestedBy={}", id, authentication.getName());
        return ResponseEntity.ok(authService.getProfile(id, authentication.getName()));
    }
    
    @PutMapping("/profile/{id}")
    @Operation(summary = "Update own profile", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<UserResponse> updateProfile(@PathVariable String id,
                                                       @Valid @RequestBody UpdateProfileRequest request,
                                                       Authentication authentication) {
        log.info("Updating profile for userId={} requestedBy={}", id, authentication.getName());
        return ResponseEntity.ok(authService.updateProfile(id, request, authentication.getName()));
    }

    @PutMapping("/admin/users/{id}")
    @Operation(summary = "Admin update any user", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> adminUpdateUser(@PathVariable String id,
                                                         @Valid @RequestBody AdminUpdateUserRequest request) {
        log.info("Admin updating userId={} targetEmail={}", id, request.getEmail());
        return ResponseEntity.ok(authService.adminUpdateUser(id, request));
    }

    @GetMapping("/admin/users")
    @Operation(summary = "Get all users", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        log.info("Fetching all users for admin view");
        return ResponseEntity.ok(authService.getAllUsers());
    }

    @PutMapping("/admin/users/{id}/manager")
    @Operation(summary = "Assign manager to a user", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> assignManager(@PathVariable String id,
                                                      @Valid @RequestBody AssignManagerRequest request) {
        log.info("Assigning managerId={} to userId={}", request.getManagerId(), id);
        return ResponseEntity.ok(authService.assignManager(id, request.getManagerId()));
    }
    
    @GetMapping("/users/{employeeId}/manager")
    @Operation(summary = "Get manager ID for employee",
               security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<String> getManagerForEmployee(@PathVariable String employeeId) {
        log.info("Fetching manager for employeeId={}", employeeId);
        return ResponseEntity.ok(authService.getManagerForEmployee(employeeId));
    }

    @GetMapping("/users/{employeeId}/manager/details")
    @Operation(summary = "Get manager details for employee",
            description = "Returns manager name and email for the given employee (self or admin).",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<UserResponse> getManagerDetails(
            @PathVariable String employeeId,
            Authentication authentication) {
        log.info("Fetching manager details for employeeId={} requestedBy={}", employeeId, authentication.getName());
        return ResponseEntity.ok(authService.getManagerDetails(employeeId, authentication.getName()));
    }
    
    @GetMapping("/greeting")
    public String Greet(){
    	return "Hello";
    }
    
    
}
