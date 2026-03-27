package com.tms.admin.dto;

import com.tms.admin.entity.ApprovalStatus;
import com.tms.admin.entity.TargetType;
import java.time.LocalDateTime;

public class ApprovalTaskResponse {
    private String id;
    private TargetType targetType;
    private String targetId;
    private String employeeId;
    private String approverId;
    private ApprovalStatus status;
    private String comments;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ApprovalTaskResponse() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public TargetType getTargetType() { return targetType; }
    public void setTargetType(TargetType targetType) { this.targetType = targetType; }

    public String getTargetId() { return targetId; }
    public void setTargetId(String targetId) { this.targetId = targetId; }

    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }

    public String getApproverId() { return approverId; }
    public void setApproverId(String approverId) { this.approverId = approverId; }

    public ApprovalStatus getStatus() { return status; }
    public void setStatus(ApprovalStatus status) { this.status = status; }

    public String getComments() { return comments; }
    public void setComments(String comments) { this.comments = comments; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
