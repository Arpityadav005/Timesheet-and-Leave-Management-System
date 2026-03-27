package com.tms.ts.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public class TimesheetEntryRequest {

    @NotNull(message = "Work date is required")
    private LocalDate workDate;

    @NotBlank(message = "Project ID is required")
    private String projectId;

    @NotNull(message = "Hours worked is required")
    @DecimalMin(value = "0.5", message = "Hours worked must be at least 0.5")
    @DecimalMax(value = "24.0", message = "Hours worked cannot exceed 24")
    private BigDecimal hoursWorked;

    @Size(max = 500, message = "Task summary cannot exceed 500 characters")
    private String taskSummary;

    @Size(max = 50, message = "Task ID cannot exceed 50 characters")
    private String taskId;

    @Size(max = 50, message = "Activity ID cannot exceed 50 characters")
    private String activityId;

    public LocalDate getWorkDate() {
        return workDate;
    }

    public void setWorkDate(LocalDate workDate) {
        this.workDate = workDate;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public BigDecimal getHoursWorked() {
        return hoursWorked;
    }

    public void setHoursWorked(BigDecimal hoursWorked) {
        this.hoursWorked = hoursWorked;
    }

    public String getTaskSummary() {
        return taskSummary;
    }

    public void setTaskSummary(String taskSummary) {
        this.taskSummary = taskSummary;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getActivityId() {
        return activityId;
    }

    public void setActivityId(String activityId) {
        this.activityId = activityId;
    }
}