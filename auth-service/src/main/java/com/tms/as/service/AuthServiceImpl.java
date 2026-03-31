package com.tms.as.service;

import com.tms.as.dto.AdminUpdateUserRequest;
import com.tms.as.dto.AuthResponse;
import com.tms.as.dto.LoginRequest;
import com.tms.as.dto.RegisterRequest;
import com.tms.as.dto.UpdateProfileRequest;
import com.tms.as.dto.UserResponse;
import com.tms.as.entity.Role;
import com.tms.as.entity.Status;
import com.tms.as.entity.User;
import com.tms.as.repository.UserRepository;
import com.tms.common.exception.ResourceAlreadyExistsException;
import com.tms.common.exception.ResourceNotFoundException;
import com.tms.common.exception.UnauthorizedException;
import com.tms.common.util.IdGeneratorUtil;
import com.tms.common.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final IdGeneratorUtil idGeneratorUtil;
    private final WelcomeEmailService welcomeEmailService;

    public AuthServiceImpl(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           JwtUtil jwtUtil,
                           IdGeneratorUtil idGeneratorUtil,
                           WelcomeEmailService welcomeEmailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.idGeneratorUtil = idGeneratorUtil;
        this.welcomeEmailService = welcomeEmailService;
    }

    @Override
    public UserResponse register(RegisterRequest request) {
        log.info("Registering employeeCode={} email={}", request.getEmployeeCode(), request.getEmail());

        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Registration rejected because email already exists: {}", request.getEmail());
            throw new ResourceAlreadyExistsException("Email already registered");
        }

        if (userRepository.existsByEmployeeCode(request.getEmployeeCode())) {
            log.warn("Registration rejected because employeeCode already exists: {}", request.getEmployeeCode());
            throw new ResourceAlreadyExistsException("Employee code already exists");
        }

        User user = new User();
        user.setId(idGeneratorUtil.generateId("USR"));
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setEmployeeCode(request.getEmployeeCode());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.EMPLOYEE);
        user.setStatus(Status.ACTIVE);

        User savedUser = userRepository.save(user);
        log.info("User registered successfully userId={} employeeCode={}", savedUser.getId(), savedUser.getEmployeeCode());
        welcomeEmailService.sendWelcomeEmail(savedUser);

        return mapToUserResponse(savedUser);
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        log.info("Authenticating email={}", request.getEmail());

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getStatus() == Status.INACTIVE) {
            log.warn("Login rejected for email={} because account is inactive", request.getEmail());
            throw new UnauthorizedException("Account is inactive");
        }

        if (user.getStatus() == Status.LOCKED) {
            log.warn("Login rejected for email={} because account is locked", request.getEmail());
            throw new UnauthorizedException("Account is locked");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("Login rejected for email={} because credentials did not match", request.getEmail());
            throw new UnauthorizedException("Invalid credentials");
        }

        String token = jwtUtil.generateToken(
                user.getEmail(),
                user.getRole().name(),
                user.getId()
        );

        return new AuthResponse(
                token,
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole().name()
        );
    }

    @Override
    @Cacheable(value = "users", key = "#id")
    public UserResponse getProfile(String id, String loggedInEmail) {
        log.info("Loading profile userId={} requestedBy={}", id, loggedInEmail);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        User loggedInUser = userRepository.findByEmail(loggedInEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!loggedInUser.getId().equals(id) && loggedInUser.getRole() != Role.ADMIN) {
            log.warn("Profile access denied for requester={} targetUserId={}", loggedInEmail, id);
            throw new UnauthorizedException("You can only view your own profile");
        }

        return mapToUserResponse(user);
    }

    @Override
    public UserResponse updateProfile(String id, UpdateProfileRequest request, String loggedInEmail) {
        log.info("Updating profile userId={} requestedBy={}", id, loggedInEmail);

        User loggedInUser = userRepository.findByEmail(loggedInEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!loggedInUser.getId().equals(id)) {
            log.warn("Profile update denied for requester={} targetUserId={}", loggedInEmail, id);
            throw new UnauthorizedException("You can only update your own profile");
        }

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!user.getEmail().equals(request.getEmail()) &&
                userRepository.existsByEmail(request.getEmail())) {
            log.warn("Profile update rejected for userId={} because email already exists: {}", id, request.getEmail());
            throw new ResourceAlreadyExistsException("Email already in use");
        }

        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());

        User savedUser = userRepository.save(user);
        log.info("Profile updated successfully for userId={}", id);

        return mapToUserResponse(savedUser);
    }

    @Override
    public UserResponse adminUpdateUser(String id, AdminUpdateUserRequest request) {
        log.info("Admin updating userId={} newEmail={} newEmployeeCode={}", id, request.getEmail(), request.getEmployeeCode());

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!user.getEmail().equals(request.getEmail()) &&
                userRepository.existsByEmail(request.getEmail())) {
            log.warn("Admin update rejected for userId={} because email already exists: {}", id, request.getEmail());
            throw new ResourceAlreadyExistsException("Email already in use");
        }

        if (!user.getEmployeeCode().equals(request.getEmployeeCode()) &&
                userRepository.existsByEmployeeCode(request.getEmployeeCode())) {
            log.warn("Admin update rejected for userId={} because employeeCode already exists: {}", id, request.getEmployeeCode());
            throw new ResourceAlreadyExistsException("Employee code already in use");
        }

        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setEmployeeCode(request.getEmployeeCode());

        if (request.getRole() != null) {
            user.setRole(request.getRole());
        }

        if (request.getStatus() != null) {
            user.setStatus(request.getStatus());
        }

        User savedUser = userRepository.save(user);
        log.info("Admin update completed for userId={} role={} status={}", savedUser.getId(), savedUser.getRole(), savedUser.getStatus());

        return mapToUserResponse(savedUser);
    }

    @Override
    public UserResponse assignManager(String id, String managerId) {
        log.info("Assigning managerId={} to userId={}", managerId, id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        User manager = userRepository.findById(managerId)
                .orElseThrow(() -> new ResourceNotFoundException("Manager not found"));

        if (user.getId().equals(manager.getId())) {
            log.warn("Manager assignment rejected because userId={} matches managerId={}", id, managerId);
            throw new UnauthorizedException("User cannot be assigned as their own manager");
        }

        if (manager.getRole() != Role.MANAGER && manager.getRole() != Role.ADMIN) {
            log.warn("Manager assignment rejected because managerId={} has role={}", managerId, manager.getRole());
            throw new UnauthorizedException("Assigned manager must have MANAGER or ADMIN role");
        }

        user.setManagerId(manager.getId());

        User savedUser = userRepository.save(user);
        log.info("Manager assignment completed for userId={} managerId={}", savedUser.getId(), managerId);

        return mapToUserResponse(savedUser);
    }

    @Override
    public String getManagerForEmployee(String employeeId) {
        log.debug("Fetching manager for employeeId={}", employeeId);
        User user = userRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));
        return user.getManagerId();
    }

    private UserResponse mapToUserResponse(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setEmployeeCode(user.getEmployeeCode());
        response.setFullName(user.getFullName());
        response.setEmail(user.getEmail());
        response.setRole(user.getRole());
        response.setStatus(user.getStatus());
        response.setCreatedAt(user.getCreatedAt());
        response.setUpdatedAt(user.getUpdatedAt());
        return response;
    }
}
