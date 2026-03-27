package com.tms.as.service;

import com.tms.as.entity.Role;
import com.tms.as.entity.Status;
import com.tms.as.entity.User;
import com.tms.as.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserCacheServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserCacheService userCacheService;

    @Test
    @DisplayName("Should return user when found by email")
    void getUserByEmail_Found() {
        User user = new User();
        user.setId("USR-ABC12345");
        user.setEmail("john@example.com");
        user.setRole(Role.EMPLOYEE);
        user.setStatus(Status.ACTIVE);

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));

        User result = userCacheService.getUserByEmail("john@example.com");

        assertNotNull(result);
        assertEquals("USR-ABC12345", result.getId());
        assertEquals("john@example.com", result.getEmail());
        verify(userRepository).findByEmail("john@example.com");
    }

    @Test
    @DisplayName("Should return null when user not found by email")
    void getUserByEmail_NotFound() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        User result = userCacheService.getUserByEmail("unknown@example.com");

        assertNull(result);
        verify(userRepository).findByEmail("unknown@example.com");
    }
}
