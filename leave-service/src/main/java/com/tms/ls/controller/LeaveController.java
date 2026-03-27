package com.tms.ls.controller;

import com.tms.ls.dto.LeaveBalanceResponse;
import com.tms.ls.dto.LeaveRequestDto;
import com.tms.ls.dto.LeaveResponse;
import com.tms.ls.dto.TeamCalendarResponse;
import com.tms.ls.service.LeaveService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping("/api/v1/leave")
@SecurityRequirement(name = "bearerAuth")
public class LeaveController {
    private static final Logger log = LoggerFactory.getLogger(LeaveController.class);

    private final LeaveService leaveService;

    public LeaveController(LeaveService leaveService) {
        this.leaveService = leaveService;
    }

    @GetMapping("/balances")
    @PreAuthorize("hasRole('EMPLOYEE') or hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<List<LeaveBalanceResponse>> getMyBalances(@io.swagger.v3.oas.annotations.Parameter(hidden = true) @RequestHeader("X-User-Id") String employeeId) {
        log.info("Fetching leave balances for employeeId={}", employeeId);
        return ResponseEntity.ok(leaveService.getBalances(employeeId));
    }

    @PostMapping("/requests")
    @PreAuthorize("hasRole('EMPLOYEE') or hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<LeaveResponse> requestLeave(@Valid @RequestBody LeaveRequestDto request,
                                                      @io.swagger.v3.oas.annotations.Parameter(hidden = true) @RequestHeader("X-User-Id") String employeeId,
                                                      @io.swagger.v3.oas.annotations.Parameter(hidden = true) @RequestHeader("Authorization") String authorization) {
        log.info("Submitting leave request for employeeId={} leaveType={} startDate={} endDate={}", employeeId, request.getLeaveType(), request.getStartDate(), request.getEndDate());
        return ResponseEntity.status(HttpStatus.CREATED).body(leaveService.requestLeave(request, employeeId, authorization));
    }

    @GetMapping("/requests")
    @PreAuthorize("hasRole('EMPLOYEE') or hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<List<LeaveResponse>> getMyRequests(@io.swagger.v3.oas.annotations.Parameter(hidden = true) @RequestHeader("X-User-Id") String employeeId) {
        log.info("Fetching leave requests for employeeId={}", employeeId);
        return ResponseEntity.ok(leaveService.getMyRequests(employeeId));
    }
    
    @PostMapping("/balances/initialize")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> initializeBalances(@RequestParam String employeeId) {
        log.info("Initializing leave balances for employeeId={}", employeeId);
        leaveService.initializeBalances(employeeId);
        return ResponseEntity.ok().build();
    }
    @GetMapping("/team-calendar")
    @PreAuthorize("hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<TeamCalendarResponse> getTeamCalendar(@io.swagger.v3.oas.annotations.Parameter(hidden = true) @RequestHeader("X-User-Id") String employeeId) {
        log.info("Fetching team calendar for managerId={}", employeeId);
        return ResponseEntity.ok(leaveService.getTeamCalendar(employeeId));
    }

    @PatchMapping("/requests/{id}/approve")
    @PreAuthorize("hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<Void> approveLeave(@PathVariable String id, @RequestBody java.util.Map<String, String> comments) {
        log.info("Approving leave request id={}", id);
        leaveService.approveLeave(id, comments);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/requests/{id}/reject")
    @PreAuthorize("hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<Void> rejectLeave(@PathVariable String id, @RequestBody java.util.Map<String, String> comments) {
        log.info("Rejecting leave request id={}", id);
        leaveService.rejectLeave(id, comments);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/requests/{id}/cancel")
    @PreAuthorize("hasRole('EMPLOYEE') or hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<Void> cancelLeaveRequest(@PathVariable String id, @io.swagger.v3.oas.annotations.Parameter(hidden = true) @RequestHeader("X-User-Id") String employeeId) {
        log.info("Cancelling leave request id={} employeeId={}", id, employeeId);
        leaveService.cancelLeaveRequest(id, employeeId);
        return ResponseEntity.ok().build();
    }
}
