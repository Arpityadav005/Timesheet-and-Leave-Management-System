package com.tms.admin.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.tms.admin.client.fallback.LeaveFallback;

@FeignClient(name = "leave-service", fallback = LeaveFallback.class)
public interface LeaveServiceClient {

    // This interface allows the Admin Service to synchronously fetch details of a leave 
    // request if a manager needs more info than what is on the ApprovalTask entity.
    @GetMapping("/api/v1/leave/requests/{id}")
    Object getLeaveRequestById(@PathVariable("id") String id);

    @org.springframework.web.bind.annotation.PatchMapping("/api/v1/leave/requests/{id}/approve")
    void approveLeave(@PathVariable("id") String id, @org.springframework.web.bind.annotation.RequestBody java.util.Map<String, String> comments);

    @org.springframework.web.bind.annotation.PatchMapping("/api/v1/leave/requests/{id}/reject")
    void rejectLeave(@PathVariable("id") String id, @org.springframework.web.bind.annotation.RequestBody java.util.Map<String, String> comments);
}
