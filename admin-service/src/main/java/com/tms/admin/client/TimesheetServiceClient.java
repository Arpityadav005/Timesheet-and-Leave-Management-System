package com.tms.admin.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.tms.admin.client.fallback.TimesheetFallback;

@FeignClient(name = "timesheet-service", fallback = TimesheetFallback.class)
public interface TimesheetServiceClient {

    // Allows the Admin Service to synchronously fetch a timesheet definition
    // if the manager needs granular details.
    @GetMapping("/api/v1/timesheets/{id}")
    Object getTimesheetById(@PathVariable("id") String id);

    @org.springframework.web.bind.annotation.PatchMapping("/api/v1/timesheets/{id}/approve")
    void approveTimesheet(@PathVariable("id") String id, @org.springframework.web.bind.annotation.RequestBody java.util.Map<String, String> comments);

    @org.springframework.web.bind.annotation.PatchMapping("/api/v1/timesheets/{id}/reject")
    void rejectTimesheet(@PathVariable("id") String id, @org.springframework.web.bind.annotation.RequestBody java.util.Map<String, String> comments);
}
