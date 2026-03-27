package com.tms.ls.controller;

import com.tms.ls.dto.LeaveBalanceResponse;
import com.tms.ls.dto.LeaveRequestDto;
import com.tms.ls.dto.LeaveResponse;
import com.tms.ls.dto.TeamCalendarResponse;
import com.tms.ls.entity.LeaveType;
import com.tms.ls.service.LeaveService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeaveControllerTest {

    @Mock
    private LeaveService leaveService;

    @InjectMocks
    private LeaveController controller;

    @Test
    void getMyBalances_ReturnsOkResponse() {
        LeaveBalanceResponse balance = new LeaveBalanceResponse();
        balance.setEmployeeId("EMP-1");
        when(leaveService.getBalances("EMP-1")).thenReturn(List.of(balance));

        ResponseEntity<List<LeaveBalanceResponse>> response = controller.getMyBalances("EMP-1");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        assertEquals("EMP-1", response.getBody().get(0).getEmployeeId());
    }

    @Test
    void requestLeave_ReturnsCreatedResponse() {
        LeaveRequestDto request = new LeaveRequestDto();
        request.setLeaveType(LeaveType.CASUAL);
        request.setStartDate(LocalDate.now().plusDays(1));
        request.setEndDate(LocalDate.now().plusDays(2));
        request.setReason("Personal");

        LeaveResponse leaveResponse = new LeaveResponse();
        leaveResponse.setId("REQ-1");

        when(leaveService.requestLeave(request, "EMP-1", "Bearer token")).thenReturn(leaveResponse);

        ResponseEntity<LeaveResponse> response = controller.requestLeave(request, "EMP-1", "Bearer token");

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals("REQ-1", response.getBody().getId());
    }

    @Test
    void getMyRequests_ReturnsOkResponse() {
        LeaveResponse leaveResponse = new LeaveResponse();
        leaveResponse.setEmployeeId("EMP-1");
        when(leaveService.getMyRequests("EMP-1")).thenReturn(List.of(leaveResponse));

        ResponseEntity<List<LeaveResponse>> response = controller.getMyRequests("EMP-1");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void initializeBalances_ReturnsOkAndDelegates() {
        ResponseEntity<Void> response = controller.initializeBalances("EMP-1");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(leaveService).initializeBalances("EMP-1");
    }

    @Test
    void getTeamCalendar_ReturnsOkResponse() {
        TeamCalendarResponse calendar = new TeamCalendarResponse(List.of(), List.of());
        when(leaveService.getTeamCalendar("MGR-1")).thenReturn(calendar);

        ResponseEntity<TeamCalendarResponse> response = controller.getTeamCalendar("MGR-1");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(calendar, response.getBody());
    }

    @Test
    void approveLeave_ReturnsOkAndDelegates() {
        Map<String, String> comments = Map.of("comments", "Approved");

        ResponseEntity<Void> response = controller.approveLeave("REQ-1", comments);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(leaveService).approveLeave("REQ-1", comments);
    }

    @Test
    void rejectLeave_ReturnsOkAndDelegates() {
        Map<String, String> comments = Map.of("comments", "Rejected");

        ResponseEntity<Void> response = controller.rejectLeave("REQ-1", comments);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(leaveService).rejectLeave("REQ-1", comments);
    }

    @Test
    void cancelLeaveRequest_ReturnsOkAndDelegates() {
        ResponseEntity<Void> response = controller.cancelLeaveRequest("REQ-1", "EMP-1");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNull(response.getBody());
        verify(leaveService).cancelLeaveRequest("REQ-1", "EMP-1");
    }
}
