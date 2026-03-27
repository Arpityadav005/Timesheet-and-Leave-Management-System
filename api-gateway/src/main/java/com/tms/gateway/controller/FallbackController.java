package com.tms.gateway.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/fallback")
public class FallbackController {
    private static final Logger log = LoggerFactory.getLogger(FallbackController.class);

    @RequestMapping("/auth")
    public ResponseEntity<Map<String, Object>> authFallback() {
        log.warn("Gateway fallback invoked for auth service");
        return buildFallbackResponse("Auth Service is temporarily unavailable");
    }

    @RequestMapping("/timesheet")
    public ResponseEntity<Map<String, Object>> timesheetFallback() {
        log.warn("Gateway fallback invoked for timesheet service");
        return buildFallbackResponse("Timesheet Service is temporarily unavailable");
    }

    @RequestMapping("/leave")
    public ResponseEntity<Map<String, Object>> leaveFallback() {
        log.warn("Gateway fallback invoked for leave service");
        return buildFallbackResponse("Leave Service is temporarily unavailable");
    }

    @RequestMapping("/admin")
    public ResponseEntity<Map<String, Object>> adminFallback() {
        log.warn("Gateway fallback invoked for admin service");
        return buildFallbackResponse("Admin Service is temporarily unavailable");
    }

    private ResponseEntity<Map<String, Object>> buildFallbackResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("code", "SERVICE_UNAVAILABLE");
        response.put("message", message);
        response.put("timestamp", LocalDateTime.now().toString());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }
}
