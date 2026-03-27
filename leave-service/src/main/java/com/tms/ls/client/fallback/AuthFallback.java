package com.tms.ls.client.fallback;

import com.tms.ls.client.AuthServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class AuthFallback implements AuthServiceClient {
    
    private static final Logger log = LoggerFactory.getLogger(AuthFallback.class);

    @Override
    public String getManagerIdForEmployee(String employeeId, String authorization) {
        log.warn("Auth Service is unreachable for getManagerIdForEmployee fallback. EmployeeId: {}", employeeId);
        return ""; // Base empty response for fallback
    }
}
