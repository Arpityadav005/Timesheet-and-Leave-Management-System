package com.tms.admin.controller;

import com.tms.admin.service.ReportingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/reports")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin Reports", description = "Administrative reporting endpoints")
public class AdminReportingController {
    private static final Logger log = LoggerFactory.getLogger(AdminReportingController.class);

    private final ReportingService reportingService;

    public AdminReportingController(ReportingService reportingService) {
        this.reportingService = reportingService;
    }

    @GetMapping("/utilization")
    @Operation(summary = "Get utilization report", description = "Returns system-wide utilization reporting for admins.")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getUtilization() {
        log.info("Fetching system utilization report");
        return ResponseEntity.ok(reportingService.getSystemUtilization());
    }
}
