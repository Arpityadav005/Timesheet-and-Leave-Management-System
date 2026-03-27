package com.tms.ts.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public class TimesheetSubmitRequest {

    @NotNull(message = "Week start date is required")
    private LocalDate weekStart;

    @Size(max = 500, message = "Comments cannot exceed 500 characters")
    private String comments;

    public LocalDate getWeekStart() {
        return weekStart;
    }

    public void setWeekStart(LocalDate weekStart) {
        this.weekStart = weekStart;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }
}