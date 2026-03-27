package com.tms.gateaway.controller;

import com.tms.gateway.controller.FallbackController;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class FallbackControllerTest {

    private final FallbackController controller = new FallbackController();

    @Test
    void authFallback_ReturnsServiceUnavailableResponse() {
        assertFallback(controller.authFallback(), "Auth Service is temporarily unavailable");
    }

    @Test
    void timesheetFallback_ReturnsServiceUnavailableResponse() {
        assertFallback(controller.timesheetFallback(), "Timesheet Service is temporarily unavailable");
    }

    @Test
    void leaveFallback_ReturnsServiceUnavailableResponse() {
        assertFallback(controller.leaveFallback(), "Leave Service is temporarily unavailable");
    }

    @Test
    void adminFallback_ReturnsServiceUnavailableResponse() {
        assertFallback(controller.adminFallback(), "Admin Service is temporarily unavailable");
    }

    private static void assertFallback(ResponseEntity<Map<String, Object>> response, String expectedMessage) {
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("SERVICE_UNAVAILABLE", response.getBody().get("code"));
        assertEquals(expectedMessage, response.getBody().get("message"));
        assertNotNull(response.getBody().get("timestamp"));
    }
}
