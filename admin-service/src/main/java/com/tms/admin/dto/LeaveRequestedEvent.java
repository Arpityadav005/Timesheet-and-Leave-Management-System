package com.tms.admin.dto;

import java.io.Serializable;
import java.time.LocalDate;

public class LeaveRequestedEvent implements Serializable {
    private String requestId;
    private String employeeId;
    private String approverId;
    private String leaveType;
    private LocalDate startDate;
    private LocalDate endDate;

    public LeaveRequestedEvent() {}

    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }

    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }

    public String getApproverId() { return approverId; }
    public void setApproverId(String approverId) { this.approverId = approverId; }

    public String getLeaveType() { return leaveType; }
    public void setLeaveType(String leaveType) { this.leaveType = leaveType; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
}
