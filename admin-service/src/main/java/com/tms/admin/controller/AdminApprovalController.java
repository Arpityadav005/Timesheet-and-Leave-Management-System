package com.tms.admin.controller;

import com.tms.admin.dto.ApprovalActionRequest;
import com.tms.admin.dto.ApprovalTaskResponse;
import com.tms.admin.service.ApprovalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/approvals")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin Approvals", description = "Approval queue actions for managers and admins")
public class AdminApprovalController {
    private static final Logger log = LoggerFactory.getLogger(AdminApprovalController.class);

    private final ApprovalService approvalService;

    public AdminApprovalController(ApprovalService approvalService) {
        this.approvalService = approvalService;
    }

    @GetMapping("/pending")
    @Operation(summary = "Get pending approvals", description = "Returns pending approval tasks assigned to the logged-in manager or admin.")
    @PreAuthorize("hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<List<ApprovalTaskResponse>> getPendingApprovals(@Parameter(hidden = true) @RequestHeader("X-User-Id") String employeeId) {
        log.info("Fetching pending approvals for approverId={}", employeeId);
        return ResponseEntity.ok(approvalService.getPendingApprovals(employeeId));
    }

    @PostMapping("/{taskId}/approve")
    @Operation(summary = "Approve a task", description = "Approves the selected approval task and publishes the completion event downstream.")
    @PreAuthorize("hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<ApprovalTaskResponse> approveTask(
            @Parameter(description = "Task ID", example = "a8bcba62-54f3-473a-b49a-01ea42f397e2")
            @PathVariable("taskId") String taskId,
            @RequestBody(required = false) ApprovalActionRequest request,
            @Parameter(hidden = true) @RequestHeader("X-User-Id") String employeeId) {
        log.info("Approving approval task taskId={} approverId={}", taskId, employeeId);
        return ResponseEntity.ok(approvalService.approveTask(
                taskId,
                request != null ? request.getComments() : null,
                employeeId));
    }

    @PostMapping("/{taskId}/reject")
    @Operation(summary = "Reject a task", description = "Rejects the selected approval task and publishes the completion event downstream.")
    @PreAuthorize("hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<ApprovalTaskResponse> rejectTask(
            @Parameter(description = "Task ID", example = "a8bcba62-54f3-473a-b49a-01ea42f397e2")
            @PathVariable("taskId") String taskId,
            @RequestBody ApprovalActionRequest request,
            @Parameter(hidden = true) @RequestHeader("X-User-Id") String employeeId) {
        log.info("Rejecting approval task taskId={} approverId={}", taskId, employeeId);
        return ResponseEntity.ok(approvalService.rejectTask(
                taskId,
                request != null ? request.getComments() : null,
                employeeId));
    }
}
