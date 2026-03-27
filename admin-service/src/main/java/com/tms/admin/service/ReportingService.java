package com.tms.admin.service;

import java.util.Map;

public interface ReportingService {
    Map<String, Object> getSystemUtilization();
    Map<String, Object> getComplianceDashboard();
    Map<String, Object> getEmployeeSummaryDashboard(String employeeId);
}
