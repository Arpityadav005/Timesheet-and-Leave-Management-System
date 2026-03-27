package com.tms.ls.service;

import com.tms.ls.dto.LeaveBalanceResponse;
import com.tms.ls.dto.LeaveRequestDto;
import com.tms.ls.dto.LeaveResponse;
import com.tms.ls.dto.LeaveResponse;
import com.tms.ls.dto.TeamCalendarResponse;

import java.util.List;

public interface LeaveService {
    List<LeaveBalanceResponse> getBalances(String employeeId);
    LeaveResponse requestLeave(LeaveRequestDto requestDto, String employeeId, String authorization);
    List<LeaveResponse> getMyRequests(String employeeId);
    TeamCalendarResponse getTeamCalendar(String managerId);
    void initializeBalances(String employeeId);
    void approveLeave(String id, java.util.Map<String, String> comments);
    void rejectLeave(String id, java.util.Map<String, String> comments);
    void cancelLeaveRequest(String id, String employeeId);
}
