package com.tms.ts.event;

import java.io.Serializable;
import java.time.LocalDate;

public class TimesheetSubmittedEvent implements Serializable {

    private String timesheetId;
    private String employeeId;
    private String approverId;
    private LocalDate weekStart;

    public TimesheetSubmittedEvent() {}

    public TimesheetSubmittedEvent(String timesheetId, String employeeId, String approverId, LocalDate weekStart) {
        this.timesheetId = timesheetId;
        this.employeeId = employeeId;
        this.approverId = approverId;
        this.weekStart = weekStart;
    }

    public String getTimesheetId() {
        return timesheetId;
    }

    public void setTimesheetId(String timesheetId) {
        this.timesheetId = timesheetId;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public String getApproverId() {
        return approverId;
    }

    public void setApproverId(String approverId) {
        this.approverId = approverId;
    }

    public LocalDate getWeekStart() {
        return weekStart;
    }

    public void setWeekStart(LocalDate weekStart) {
        this.weekStart = weekStart;
    }
}