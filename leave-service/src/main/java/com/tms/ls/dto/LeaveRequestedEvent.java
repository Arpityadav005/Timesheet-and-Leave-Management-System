package com.tms.ls.dto;

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

    public LeaveRequestedEvent(String requestId, String employeeId, String approverId, String leaveType, LocalDate startDate, LocalDate endDate) {
        this.requestId = requestId;
        this.employeeId = employeeId;
        this.approverId = approverId;
        this.leaveType = leaveType;
        this.startDate = startDate;
        this.endDate = endDate;
    }

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
