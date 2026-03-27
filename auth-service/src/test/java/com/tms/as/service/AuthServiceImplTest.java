package com.tms.as.service;

import com.tms.as.dto.*;
import com.tms.as.entity.Role;
import com.tms.as.entity.Status;
import com.tms.as.entity.User;
import com.tms.as.repository.UserRepository;
import com.tms.common.exception.ResourceAlreadyExistsException;
import com.tms.common.exception.ResourceNotFoundException;
import com.tms.common.exception.UnauthorizedException;
import com.tms.common.util.IdGeneratorUtil;
import com.tms.common.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private IdGeneratorUtil idGeneratorUtil;

    @InjectMocks
    private AuthServiceImpl authService;

    private User testUser;
    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId("USR-ABC12345");
        testUser.setFullName("John Doe");
        testUser.setEmail("john@example.com");
        testUser.setEmployeeCode("EMP-001");
        testUser.setPassword("encodedPassword");
        testUser.setRole(Role.EMPLOYEE);
        testUser.setStatus(Status.ACTIVE);

        registerRequest = new RegisterRequest();
        registerRequest.setFullName("John Doe");
        registerRequest.setEmail("john@example.com");
        registerRequest.setEmployeeCode("EMP-001");
        registerRequest.setPassword("Password1!");

        loginRequest = new LoginRequest();
        loginRequest.setEmail("john@example.com");
        loginRequest.setPassword("Password1!");
    }

    // ==================== REGISTER TESTS ====================

    @Nested
    @DisplayName("Register Tests")
    class RegisterTests {

        @Test
        @DisplayName("Should register successfully with valid data")
        void register_Success() {
            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(userRepository.existsByEmployeeCode(anyString())).thenReturn(false);
            when(idGeneratorUtil.generateId("USR")).thenReturn("USR-ABC12345");
            when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            UserResponse response = authService.register(registerRequest);

            assertNotNull(response);
            assertEquals("John Doe", response.getFullName());
            assertEquals("john@example.com", response.getEmail());
            assertEquals("EMP-001", response.getEmployeeCode());
            assertEquals(Role.EMPLOYEE, response.getRole());
            assertEquals(Status.ACTIVE, response.getStatus());

            verify(userRepository).existsByEmail("john@example.com");
            verify(userRepository).existsByEmployeeCode("EMP-001");
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("Should throw exception when email already exists")
        void register_DuplicateEmail() {
            when(userRepository.existsByEmail("john@example.com")).thenReturn(true);

            ResourceAlreadyExistsException exception = assertThrows(
                    ResourceAlreadyExistsException.class,
                    () -> authService.register(registerRequest)
            );

            assertEquals("Email already registered", exception.getMessage());
            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("Should throw exception when employee code already exists")
        void register_DuplicateEmployeeCode() {
            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(userRepository.existsByEmployeeCode("EMP-001")).thenReturn(true);

            ResourceAlreadyExistsException exception = assertThrows(
                    ResourceAlreadyExistsException.class,
                    () -> authService.register(registerRequest)
            );

            assertEquals("Employee code already exists", exception.getMessage());
            verify(userRepository, never()).save(any(User.class));
        }
    }

    // ==================== LOGIN TESTS ====================

    @Nested
    @DisplayName("Login Tests")
    class LoginTests {

        @Test
        @DisplayName("Should login successfully with valid credentials")
        void login_Success() {
            when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("Password1!", "encodedPassword")).thenReturn(true);
            when(jwtUtil.generateToken("john@example.com", "EMPLOYEE", "USR-ABC12345"))
                    .thenReturn("jwt-token");

            AuthResponse response = authService.login(loginRequest);

            assertNotNull(response);
            assertEquals("jwt-token", response.getToken());
            assertEquals("USR-ABC12345", response.getId());
            assertEquals("john@example.com", response.getEmail());
            assertEquals("John Doe", response.getFullName());
            assertEquals("EMPLOYEE", response.getRole());
        }

        @Test
        @DisplayName("Should throw exception when user not found")
        void login_UserNotFound() {
            when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.empty());

            ResourceNotFoundException exception = assertThrows(
                    ResourceNotFoundException.class,
                    () -> authService.login(loginRequest)
            );

            assertEquals("User not found", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when account is inactive")
        void login_InactiveAccount() {
            testUser.setStatus(Status.INACTIVE);
            when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));

            UnauthorizedException exception = assertThrows(
                    UnauthorizedException.class,
                    () -> authService.login(loginRequest)
            );

            assertEquals("Account is inactive", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when account is locked")
        void login_LockedAccount() {
            testUser.setStatus(Status.LOCKED);
            when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));

            UnauthorizedException exception = assertThrows(
                    UnauthorizedException.class,
                    () -> authService.login(loginRequest)
            );

            assertEquals("Account is locked", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when password is wrong")
        void login_WrongPassword() {
            when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("Password1!", "encodedPassword")).thenReturn(false);

            UnauthorizedException exception = assertThrows(
                    UnauthorizedException.class,
                    () -> authService.login(loginRequest)
            );

            assertEquals("Invalid credentials", exception.getMessage());
        }
    }

    // ==================== GET PROFILE TESTS ====================

    @Nested
    @DisplayName("Get Profile Tests")
    class GetProfileTests {

        @Test
        @DisplayName("Should return own profile successfully")
        void getProfile_OwnProfile() {
            when(userRepository.findById("USR-ABC12345")).thenReturn(Optional.of(testUser));
            when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));

            UserResponse response = authService.getProfile("USR-ABC12345", "john@example.com");

            assertNotNull(response);
            assertEquals("USR-ABC12345", response.getId());
            assertEquals("john@example.com", response.getEmail());
        }

        @Test
        @DisplayName("Should allow admin to view another user's profile")
        void getProfile_AdminViewsOther() {
            User adminUser = new User();
            adminUser.setId("USR-ADMIN001");
            adminUser.setEmail("admin@example.com");
            adminUser.setRole(Role.ADMIN);

            when(userRepository.findById("USR-ABC12345")).thenReturn(Optional.of(testUser));
            when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(adminUser));

            UserResponse response = authService.getProfile("USR-ABC12345", "admin@example.com");

            assertNotNull(response);
            assertEquals("USR-ABC12345", response.getId());
        }

        @Test
        @DisplayName("Should throw exception when non-admin views another profile")
        void getProfile_NonAdminViewsOther() {
            User otherUser = new User();
            otherUser.setId("USR-OTHER001");
            otherUser.setEmail("other@example.com");
            otherUser.setRole(Role.EMPLOYEE);

            when(userRepository.findById("USR-ABC12345")).thenReturn(Optional.of(testUser));
            when(userRepository.findByEmail("other@example.com")).thenReturn(Optional.of(otherUser));

            UnauthorizedException exception = assertThrows(
                    UnauthorizedException.class,
                    () -> authService.getProfile("USR-ABC12345", "other@example.com")
            );

            assertEquals("You can only view your own profile", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when user not found")
        void getProfile_UserNotFound() {
            when(userRepository.findById("USR-UNKNOWN")).thenReturn(Optional.empty());

            assertThrows(
                    ResourceNotFoundException.class,
                    () -> authService.getProfile("USR-UNKNOWN", "john@example.com")
            );
        }
    }

    // ==================== UPDATE PROFILE TESTS ====================

    @Nested
    @DisplayName("Update Profile Tests")
    class UpdateProfileTests {

        @Test
        @DisplayName("Should update own profile successfully")
        void updateProfile_Success() {
            UpdateProfileRequest request = new UpdateProfileRequest();
            request.setFullName("John Updated");
            request.setEmail("john@example.com");

            User updatedUser = new User();
            updatedUser.setId("USR-ABC12345");
            updatedUser.setFullName("John Updated");
            updatedUser.setEmail("john@example.com");
            updatedUser.setEmployeeCode("EMP-001");
            updatedUser.setRole(Role.EMPLOYEE);
            updatedUser.setStatus(Status.ACTIVE);

            when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));
            when(userRepository.findById("USR-ABC12345")).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenReturn(updatedUser);

            UserResponse response = authService.updateProfile("USR-ABC12345", request, "john@example.com");

            assertNotNull(response);
            assertEquals("John Updated", response.getFullName());
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("Should throw exception when updating someone else's profile")
        void updateProfile_UnauthorizedUpdate() {
            UpdateProfileRequest request = new UpdateProfileRequest();
            request.setFullName("Hacker");
            request.setEmail("hacker@example.com");

            User otherUser = new User();
            otherUser.setId("USR-OTHER001");
            otherUser.setEmail("other@example.com");

            when(userRepository.findByEmail("other@example.com")).thenReturn(Optional.of(otherUser));

            assertThrows(
                    UnauthorizedException.class,
                    () -> authService.updateProfile("USR-ABC12345", request, "other@example.com")
            );

            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("Should throw exception when new email already in use")
        void updateProfile_EmailAlreadyInUse() {
            UpdateProfileRequest request = new UpdateProfileRequest();
            request.setFullName("John Doe");
            request.setEmail("taken@example.com");

            when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));
            when(userRepository.findById("USR-ABC12345")).thenReturn(Optional.of(testUser));
            when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);

            assertThrows(
                    ResourceAlreadyExistsException.class,
                    () -> authService.updateProfile("USR-ABC12345", request, "john@example.com")
            );

            verify(userRepository, never()).save(any(User.class));
        }
    }

    // ==================== ADMIN UPDATE USER TESTS ====================

    @Nested
    @DisplayName("Admin Update User Tests")
    class AdminUpdateUserTests {

        @Test
        @DisplayName("Should admin update user with role and status change")
        void adminUpdateUser_Success() {
            AdminUpdateUserRequest request = new AdminUpdateUserRequest();
            request.setFullName("John Admin Updated");
            request.setEmail("john@example.com");
            request.setEmployeeCode("EMP-001");
            request.setRole(Role.MANAGER);
            request.setStatus(Status.INACTIVE);

            User updatedUser = new User();
            updatedUser.setId("USR-ABC12345");
            updatedUser.setFullName("John Admin Updated");
            updatedUser.setEmail("john@example.com");
            updatedUser.setEmployeeCode("EMP-001");
            updatedUser.setRole(Role.MANAGER);
            updatedUser.setStatus(Status.INACTIVE);

            when(userRepository.findById("USR-ABC12345")).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenReturn(updatedUser);

            UserResponse response = authService.adminUpdateUser("USR-ABC12345", request);

            assertNotNull(response);
            assertEquals("John Admin Updated", response.getFullName());
            assertEquals(Role.MANAGER, response.getRole());
            assertEquals(Status.INACTIVE, response.getStatus());
            verify(userRepository).save(any(User.class));
        }
    }
}
