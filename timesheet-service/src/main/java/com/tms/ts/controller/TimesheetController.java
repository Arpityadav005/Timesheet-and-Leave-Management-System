package com.tms.ts.controller;

import com.tms.ts.dto.*;
import com.tms.ts.service.TimesheetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/timesheets")
@Tag(name = "Timesheet", description = "Timesheet management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class TimesheetController {
    private static final Logger log = LoggerFactory.getLogger(TimesheetController.class);

    private final TimesheetService timesheetService;

    public TimesheetController(TimesheetService timesheetService) {
        this.timesheetService = timesheetService;
    }

    @PostMapping("/entries")
    @Operation(summary = "Add a timesheet entry")
    @PreAuthorize("hasRole('EMPLOYEE') or hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<TimesheetEntryResponse> addEntry(
            @Valid @RequestBody TimesheetEntryRequest request,
            @Parameter(hidden = true, name = "X-User-Id") @RequestHeader("X-User-Id") String employeeId,
            @Parameter(hidden = true, name = "Authorization") @RequestHeader("Authorization") String authorization) {
        log.info("Adding timesheet entry for employeeId={} workDate={} projectId={}", employeeId, request.getWorkDate(), request.getProjectId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(timesheetService.addEntry(request, employeeId, authorization));
    }

    @PutMapping("/entries/{entryId}")
    @Operation(summary = "Update a timesheet entry")
    @PreAuthorize("hasRole('EMPLOYEE') or hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<TimesheetEntryResponse> updateEntry(
            @Parameter(description = "Timesheet entry ID", example = "TSE-12345678")
            @PathVariable("entryId") String entryId,
            @Valid @RequestBody TimesheetEntryRequest request,
            @Parameter(hidden = true, name = "X-User-Id") @RequestHeader("X-User-Id") String employeeId) {
        log.info("Updating timesheet entry entryId={} employeeId={}", entryId, employeeId);
        return ResponseEntity.ok(timesheetService.updateEntry(entryId, request, employeeId));
    }

    @DeleteMapping("/entries/{entryId}")
    @Operation(summary = "Delete a timesheet entry")
    @PreAuthorize("hasRole('EMPLOYEE') or hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<Void> deleteEntry(
            @Parameter(description = "Timesheet entry ID", example = "TSE-12345678")
            @PathVariable("entryId") String entryId,
            @Parameter(hidden = true, name = "X-User-Id") @RequestHeader("X-User-Id") String employeeId) {
        log.info("Deleting timesheet entry entryId={} employeeId={}", entryId, employeeId);
        timesheetService.deleteEntry(entryId, employeeId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @Operation(summary = "Get timesheet for a specific week")
    @PreAuthorize("hasRole('EMPLOYEE') or hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<TimesheetResponse> getTimesheet(
            @Parameter(name = "date", description = "Any date within the week to fetch", example = "2026-03-30")
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Parameter(hidden = true, name = "X-User-Id") @RequestHeader("X-User-Id") String employeeId) {
        LocalDate weekStart = date.with(DayOfWeek.MONDAY);
        log.info("Fetching timesheet for employeeId={} weekStart={}", employeeId, weekStart);
        return ResponseEntity.ok(timesheetService.getTimesheet(weekStart, employeeId));
    }

    @GetMapping("/weeks/{date}/validate")
    @Operation(summary = "Validate a timesheet week before submission")
    @PreAuthorize("hasRole('EMPLOYEE') or hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<TimesheetValidationResponse> validateTimesheet(
            @Parameter(name = "date", description = "Any date within the week to validate", example = "2026-03-30")
            @PathVariable("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Parameter(hidden = true, name = "X-User-Id") @RequestHeader("X-User-Id") String employeeId) {
        LocalDate weekStart = date.with(DayOfWeek.MONDAY);
        log.info("Validating timesheet for employeeId={} weekStart={}", employeeId, weekStart);
        return ResponseEntity.ok(timesheetService.validateTimesheet(weekStart, employeeId));
    }

    @GetMapping("/all")
    @Operation(summary = "Get all timesheets for logged in employee")
    @PreAuthorize("hasRole('EMPLOYEE') or hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<List<TimesheetResponse>> getAllTimesheets(
            @Parameter(hidden = true, name = "X-User-Id") @RequestHeader("X-User-Id") String employeeId) {
        log.info("Fetching all timesheets for employeeId={}", employeeId);
        return ResponseEntity.ok(timesheetService.getAllTimesheets(employeeId));
    }

    @PostMapping("/submit")
    @Operation(summary = "Submit timesheet for approval")
    @PreAuthorize("hasRole('EMPLOYEE') or hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<TimesheetResponse> submitTimesheet(
            @Valid @RequestBody TimesheetSubmitRequest request,
            @Parameter(hidden = true, name = "X-User-Id") @RequestHeader("X-User-Id") String employeeId,
            @Parameter(hidden = true, name = "Authorization") @RequestHeader("Authorization") String authorization) {
        log.info("Submitting timesheet for employeeId={} weekStart={}", employeeId, request.getWeekStart());
        return ResponseEntity.ok(timesheetService.submitTimesheet(request, employeeId, authorization));
    }

    @PatchMapping("/{id}/approve")
    @Operation(summary = "Approve a timesheet")
    @PreAuthorize("hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<Void> approveTimesheet(
            @Parameter(description = "Timesheet ID", example = "TS-12345678")
            @PathVariable("id") String id,
            @RequestBody(required = false) ApprovalActionRequest request) {
        log.info("Approving timesheet id={}", id);
        timesheetService.approveTimesheet(id, toCommentsMap(request));
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{id}/reject")
    @Operation(summary = "Reject a timesheet")
    @PreAuthorize("hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<Void> rejectTimesheet(
            @Parameter(description = "Timesheet ID", example = "TS-12345678")
            @PathVariable("id") String id,
            @RequestBody(required = false) ApprovalActionRequest request) {
        log.info("Rejecting timesheet id={}", id);
        timesheetService.rejectTimesheet(id, toCommentsMap(request));
        return ResponseEntity.ok().build();
    }

    private Map<String, String> toCommentsMap(ApprovalActionRequest request) {
        Map<String, String> comments = new HashMap<>();
        if (request != null && request.getComments() != null) {
            comments.put("comments", request.getComments());
        }
        return comments;
    }
}
