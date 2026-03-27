package com.tms.ts.client;

import com.tms.ts.client.fallback.AuthFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "auth-service", fallback = AuthFallback.class)
public interface AuthServiceClient {

    @GetMapping("/api/v1/auth/users/{employeeId}/manager")
    String getManagerIdForEmployee(@PathVariable("employeeId") String employeeId,
                                   @RequestHeader("Authorization") String authorization);
}
