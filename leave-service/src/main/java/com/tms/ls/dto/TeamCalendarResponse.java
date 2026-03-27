package com.tms.ls.dto;

import java.util.List;

public class TeamCalendarResponse {
    private List<LeaveResponse> teamLeaves;
    private List<HolidayResponse> holidays;

    public TeamCalendarResponse() {
    }

    public TeamCalendarResponse(List<LeaveResponse> teamLeaves, List<HolidayResponse> holidays) {
        this.teamLeaves = teamLeaves;
        this.holidays = holidays;
    }

    public List<LeaveResponse> getTeamLeaves() {
        return teamLeaves;
    }

    public void setTeamLeaves(List<LeaveResponse> teamLeaves) {
        this.teamLeaves = teamLeaves;
    }

    public List<HolidayResponse> getHolidays() {
        return holidays;
    }

    public void setHolidays(List<HolidayResponse> holidays) {
        this.holidays = holidays;
    }
}
