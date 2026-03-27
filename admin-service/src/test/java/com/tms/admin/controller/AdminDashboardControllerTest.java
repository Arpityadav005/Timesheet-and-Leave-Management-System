package com.tms.admin.controller;

import com.tms.admin.service.ReportingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminDashboardControllerTest {

    @Mock
    private ReportingService reportingService;

    @InjectMocks
    private AdminDashboardController controller;

    @Test
    void getComplianceDashboard_ReturnsOkResponse() {
        Map<String, Object> dashboard = Map.of("openApprovals", 3);
        when(reportingService.getComplianceDashboard()).thenReturn(dashboard);

        ResponseEntity<Map<String, Object>> response = controller.getComplianceDashboard();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(3, response.getBody().get("openApprovals"));
    }

    @Test
    void getEmployeeSummaryDashboard_ReturnsOkResponse() {
        Map<String, Object> dashboard = Map.of("employeeId", "EMP-1");
        when(reportingService.getEmployeeSummaryDashboard("EMP-1")).thenReturn(dashboard);

        ResponseEntity<Map<String, Object>> response = controller.getEmployeeSummaryDashboard("EMP-1");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("EMP-1", response.getBody().get("employeeId"));
    }
}
