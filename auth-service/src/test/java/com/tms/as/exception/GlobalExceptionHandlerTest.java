package com.tms.as.exception;

import com.tms.common.exception.ErrorResponse;
import com.tms.common.exception.ResourceAlreadyExistsException;
import com.tms.common.exception.ResourceNotFoundException;
import com.tms.common.exception.UnauthorizedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("Should handle ResourceNotFoundException with 404")
    void handleResourceNotFoundException() {
        ResourceNotFoundException ex = new ResourceNotFoundException("User not found");

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleResourceNotFoundException(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("NOT_FOUND", response.getBody().getCode());
        assertEquals("User not found", response.getBody().getMessage());
    }

    @Test
    @DisplayName("Should handle ResourceAlreadyExistsException with 409")
    void handleResourceAlreadyExistsException() {
        ResourceAlreadyExistsException ex = new ResourceAlreadyExistsException("Email already registered");

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleResourceAlreadyExistsException(ex);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("CONFLICT", response.getBody().getCode());
        assertEquals("Email already registered", response.getBody().getMessage());
    }

    @Test
    @DisplayName("Should handle UnauthorizedException with 401")
    void handleUnauthorizedException() {
        UnauthorizedException ex = new UnauthorizedException("Invalid credentials");

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleUnauthorizedException(ex);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("UNAUTHORIZED", response.getBody().getCode());
        assertEquals("Invalid credentials", response.getBody().getMessage());
    }

    @Test
    @DisplayName("Should handle AccessDeniedException with 403")
    void handleAccessDeniedException() {
        AccessDeniedException ex = new AccessDeniedException("Access denied");

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleAccessDeniedException(ex);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("FORBIDDEN", response.getBody().getCode());
        assertEquals("Only admin can perform this action", response.getBody().getMessage());
    }
}
