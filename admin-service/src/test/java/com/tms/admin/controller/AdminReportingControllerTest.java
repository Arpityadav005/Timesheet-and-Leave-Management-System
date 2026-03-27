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
class AdminReportingControllerTest {

    @Mock
    private ReportingService reportingService;

    @InjectMocks
    private AdminReportingController controller;

    @Test
    void getUtilization_ReturnsOkResponse() {
        Map<String, Object> report = Map.of("utilizationRate", 78);
        when(reportingService.getSystemUtilization()).thenReturn(report);

        ResponseEntity<Map<String, Object>> response = controller.getUtilization();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(78, response.getBody().get("utilizationRate"));
    }
}
