package com.tms.admin.service;

import com.tms.admin.entity.ApprovalStatus;
import com.tms.admin.entity.TargetType;
import com.tms.admin.repository.ApprovalTaskRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportingServiceImplTest {

    @Mock
    private ApprovalTaskRepository approvalTaskRepository;

    @InjectMocks
    private ReportingServiceImpl service;

    @Test
    void getSystemUtilization_ReturnsAggregatedMetrics() {
        when(approvalTaskRepository.count()).thenReturn(10L);
        when(approvalTaskRepository.countByStatus(ApprovalStatus.PENDING)).thenReturn(2L);
        when(approvalTaskRepository.countByStatus(ApprovalStatus.APPROVED)).thenReturn(5L);
        when(approvalTaskRepository.countByStatus(ApprovalStatus.REJECTED)).thenReturn(3L);
        when(approvalTaskRepository.countByTargetType(TargetType.LEAVE)).thenReturn(6L);
        when(approvalTaskRepository.countByTargetType(TargetType.TIMESHEET)).thenReturn(4L);

        Map<String, Object> metrics = service.getSystemUtilization();

        assertEquals(10L, metrics.get("totalApprovalTasks"));
        assertEquals(2L, metrics.get("pendingApprovalTasks"));
        assertEquals(5L, metrics.get("approvedApprovalTasks"));
        assertEquals(3L, metrics.get("rejectedApprovalTasks"));
        assertEquals(6L, metrics.get("leaveApprovalTasks"));
        assertEquals(4L, metrics.get("timesheetApprovalTasks"));
        assertEquals("80.00%", metrics.get("approvalCompletionRate"));
    }

    @Test
    void getSystemUtilization_UsesZeroPercentWhenNoTasksExist() {
        when(approvalTaskRepository.count()).thenReturn(0L);
        when(approvalTaskRepository.countByStatus(ApprovalStatus.PENDING)).thenReturn(0L);
        when(approvalTaskRepository.countByStatus(ApprovalStatus.APPROVED)).thenReturn(0L);
        when(approvalTaskRepository.countByStatus(ApprovalStatus.REJECTED)).thenReturn(0L);
        when(approvalTaskRepository.countByTargetType(TargetType.LEAVE)).thenReturn(0L);
        when(approvalTaskRepository.countByTargetType(TargetType.TIMESHEET)).thenReturn(0L);

        Map<String, Object> metrics = service.getSystemUtilization();

        assertEquals("0.00%", metrics.get("approvalCompletionRate"));
    }

    @Test
    void getComplianceDashboard_ReturnsAggregatedMetrics() {
        when(approvalTaskRepository.count()).thenReturn(8L);
        when(approvalTaskRepository.countByStatus(ApprovalStatus.PENDING)).thenReturn(3L);
        when(approvalTaskRepository.countByStatus(ApprovalStatus.APPROVED)).thenReturn(4L);
        when(approvalTaskRepository.countByStatus(ApprovalStatus.REJECTED)).thenReturn(1L);
        when(approvalTaskRepository.countByTargetTypeAndStatus(TargetType.LEAVE, ApprovalStatus.PENDING)).thenReturn(2L);
        when(approvalTaskRepository.countByTargetTypeAndStatus(TargetType.TIMESHEET, ApprovalStatus.PENDING)).thenReturn(1L);

        Map<String, Object> metrics = service.getComplianceDashboard();

        assertEquals(3L, metrics.get("pendingApprovalsSystemWide"));
        assertEquals(4L, metrics.get("approvedApprovalsSystemWide"));
        assertEquals(1L, metrics.get("rejectedApprovalsSystemWide"));
        assertEquals(2L, metrics.get("pendingLeaveApprovals"));
        assertEquals(1L, metrics.get("pendingTimesheetApprovals"));
        assertEquals("62.50%", metrics.get("approvalCompletionRate"));
    }

    @Test
    void getEmployeeSummaryDashboard_ReturnsEmployeeMetrics() {
        when(approvalTaskRepository.countByEmployeeId("EMP-1")).thenReturn(5L);
        when(approvalTaskRepository.countByEmployeeIdAndStatus("EMP-1", ApprovalStatus.PENDING)).thenReturn(2L);
        when(approvalTaskRepository.countByEmployeeIdAndStatus("EMP-1", ApprovalStatus.APPROVED)).thenReturn(2L);
        when(approvalTaskRepository.countByEmployeeIdAndStatus("EMP-1", ApprovalStatus.REJECTED)).thenReturn(1L);
        when(approvalTaskRepository.countByEmployeeIdAndTargetType("EMP-1", TargetType.LEAVE)).thenReturn(3L);
        when(approvalTaskRepository.countByEmployeeIdAndTargetType("EMP-1", TargetType.TIMESHEET)).thenReturn(2L);

        Map<String, Object> metrics = service.getEmployeeSummaryDashboard("EMP-1");

        assertEquals(5L, metrics.get("myTotalRequests"));
        assertEquals(2L, metrics.get("myPendingRequests"));
        assertEquals(2L, metrics.get("myApprovedRequests"));
        assertEquals(1L, metrics.get("myRejectedRequests"));
        assertEquals(3L, metrics.get("myLeaveRequests"));
        assertEquals(2L, metrics.get("myTimesheetRequests"));
    }
}
