package com.tms.admin.dto;

import java.io.Serializable;

public class ApprovalCompletedEvent implements Serializable {
    private String targetType; // TIMESHEET, LEAVE
    private String targetId;
    private String employeeId;
    private String status; // APPROVED, REJECTED
    private String comments;

    public ApprovalCompletedEvent() {}

    public ApprovalCompletedEvent(String targetType, String targetId, String employeeId, String status, String comments) {
        this.targetType = targetType;
        this.targetId = targetId;
        this.employeeId = employeeId;
        this.status = status;
        this.comments = comments;
    }

    public String getTargetType() { return targetType; }
    public void setTargetType(String targetType) { this.targetType = targetType; }

    public String getTargetId() { return targetId; }
    public void setTargetId(String targetId) { this.targetId = targetId; }
    
    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getComments() { return comments; }
    public void setComments(String comments) { this.comments = comments; }
}
