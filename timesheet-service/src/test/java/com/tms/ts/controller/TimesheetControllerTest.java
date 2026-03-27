package com.tms.ts.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tms.ts.dto.TimesheetEntryRequest;
import com.tms.ts.dto.TimesheetEntryResponse;
import com.tms.ts.dto.TimesheetResponse;
import com.tms.ts.dto.TimesheetSubmitRequest;
import com.tms.ts.dto.TimesheetValidationResponse;
import com.tms.ts.dto.ApprovalActionRequest;
import com.tms.ts.entity.TimesheetStatus;
import com.tms.ts.security.JwtAuthenticationFilter;
import com.tms.ts.service.TimesheetService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.ArrayList;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = TimesheetController.class, properties = {
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false",
        "JWT_SECRET=01234567890123456789012345678901"
})
@AutoConfigureMockMvc(addFilters = false) // Disable security filters for simple unit testing
class TimesheetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TimesheetService timesheetService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private org.springframework.amqp.rabbit.connection.ConnectionFactory connectionFactory;

    @MockBean
    private com.tms.ts.repository.ProjectRepository projectRepository;

    @MockBean
    private com.tms.ts.repository.TimesheetRepository timesheetRepository;

    @MockBean
    private com.tms.ts.repository.TimesheetEntryRepository timesheetEntryRepository;

    private TimesheetEntryResponse entryResponse;
    private TimesheetResponse timesheetResponse;
    private TimesheetValidationResponse validationResponse;
    private final LocalDate TODAY = LocalDate.now();

    private org.springframework.security.authentication.UsernamePasswordAuthenticationToken getAuth() {
        return new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                "employee@test.com", "USR-123", List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_EMPLOYEE"))
        );
    }

    @BeforeEach
    void setUp() {
        entryResponse = new TimesheetEntryResponse();
        entryResponse.setId("TE-001");
        entryResponse.setProjectId("PRJ-101");
        entryResponse.setProjectName("Test Project");
        entryResponse.setHoursWorked(new BigDecimal("8.0"));

        timesheetResponse = new TimesheetResponse();
        timesheetResponse.setId("TS-001");
        timesheetResponse.setStatus(TimesheetStatus.DRAFT);
        timesheetResponse.setEntries(List.of(entryResponse));
        
        validationResponse = new TimesheetValidationResponse(true, new ArrayList<>(), new BigDecimal("40.0"));
    }

    @Test
    @WithMockUser(username = "USR-123", roles = {"EMPLOYEE"})
    @DisplayName("POST /entries - Should add a new entry")
    void addEntry_Success() throws Exception {
        TimesheetEntryRequest request = new TimesheetEntryRequest();
        request.setProjectId("PRJ-101");
        request.setWorkDate(TODAY);
        request.setHoursWorked(new BigDecimal("8.0"));
        request.setTaskSummary("Did some work");

        when(timesheetService.addEntry(any(TimesheetEntryRequest.class), anyString(), anyString()))
                .thenReturn(entryResponse);

        mockMvc.perform(post("/api/v1/timesheets/entries")
                        .principal(getAuth())
                        .header("X-User-Id", "USR-123")
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("TE-001"))
                .andExpect(jsonPath("$.projectName").value("Test Project"));
    }

    @Test
    @WithMockUser(username = "USR-123", roles = {"EMPLOYEE"})
    @DisplayName("PUT /entries/{entryId} - Should update an entry")
    void updateEntry_Success() throws Exception {
        TimesheetEntryRequest request = new TimesheetEntryRequest();
        request.setProjectId("PRJ-101");
        request.setWorkDate(TODAY);
        request.setHoursWorked(new BigDecimal("4.0"));

        entryResponse.setHoursWorked(new BigDecimal("4.0"));

        when(timesheetService.updateEntry(anyString(), any(TimesheetEntryRequest.class), anyString()))
                .thenReturn(entryResponse);

        mockMvc.perform(put("/api/v1/timesheets/entries/TE-001")
                        .principal(getAuth())
                        .header("X-User-Id", "USR-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hoursWorked").value(4.0));
    }

    @Test
    @WithMockUser(username = "USR-123", roles = {"EMPLOYEE"})
    @DisplayName("DELETE /entries/{entryId} - Should delete an entry")
    void deleteEntry_Success() throws Exception {
        mockMvc.perform(delete("/api/v1/timesheets/entries/TE-001")
                        .principal(getAuth())
                        .header("X-User-Id", "USR-123"))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "USR-123", roles = {"EMPLOYEE"})
    @DisplayName("GET /api/v1/timesheets - Should fetch timesheet by date")
    void getTimesheet_Success() throws Exception {
        when(timesheetService.getTimesheet(any(LocalDate.class), anyString()))
                .thenReturn(timesheetResponse);

        mockMvc.perform(get("/api/v1/timesheets?date=" + TODAY.toString())
                        .principal(getAuth())
                        .header("X-User-Id", "USR-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("TS-001"));

        verify(timesheetService).getTimesheet(TODAY.with(java.time.DayOfWeek.MONDAY), "USR-123");
    }

    @Test
    @WithMockUser(username = "USR-123", roles = {"EMPLOYEE"})
    @DisplayName("GET /weeks/{date}/validate - Should successfully validate timesheet")
    void validateTimesheet_Success() throws Exception {
        when(timesheetService.validateTimesheet(any(LocalDate.class), anyString()))
                .thenReturn(validationResponse);

        mockMvc.perform(get("/api/v1/timesheets/weeks/" + TODAY.toString() + "/validate")
                        .principal(getAuth())
                        .header("X-User-Id", "USR-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.totalWeeklyHours").value(40.0));

        verify(timesheetService).validateTimesheet(TODAY.with(java.time.DayOfWeek.MONDAY), "USR-123");
    }

    @Test
    @WithMockUser(username = "USR-123", roles = {"EMPLOYEE"})
    @DisplayName("GET /all - Should fetch all timesheets for employee")
    void getAllTimesheets_Success() throws Exception {
        when(timesheetService.getAllTimesheets(anyString()))
                .thenReturn(List.of(timesheetResponse));

        mockMvc.perform(get("/api/v1/timesheets/all")
                        .principal(getAuth())
                        .header("X-User-Id", "USR-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value("TS-001"));
    }

    @Test
    @WithMockUser(username = "USR-123", roles = {"EMPLOYEE"})
    @DisplayName("POST /submit - Should submit a timesheet")
    void submitTimesheet_Success() throws Exception {
        TimesheetSubmitRequest request = new TimesheetSubmitRequest();
        request.setWeekStart(TODAY);
        request.setComments("Week done");

        timesheetResponse.setStatus(TimesheetStatus.SUBMITTED);

        when(timesheetService.submitTimesheet(any(TimesheetSubmitRequest.class), anyString(), anyString()))
                .thenReturn(timesheetResponse);

        mockMvc.perform(post("/api/v1/timesheets/submit")
                        .principal(getAuth())
                        .header("X-User-Id", "USR-123")
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUBMITTED"));
    }

    @Test
    @WithMockUser(username = "USR-123", roles = {"MANAGER"})
    @DisplayName("PATCH /{id}/approve - Should approve timesheet with comments")
    void approveTimesheet_Success() throws Exception {
        ApprovalActionRequest request = new ApprovalActionRequest();
        request.setComments("Approved");

        mockMvc.perform(patch("/api/v1/timesheets/TS-001/approve")
                        .principal(getAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(timesheetService).approveTimesheet(
                anyString(),
                argThat(map -> "Approved".equals(map.get("comments"))));
    }

    @Test
    @WithMockUser(username = "USR-123", roles = {"MANAGER"})
    @DisplayName("PATCH /{id}/reject - Should reject timesheet without comments body")
    void rejectTimesheet_Success() throws Exception {
        mockMvc.perform(patch("/api/v1/timesheets/TS-001/reject")
                        .principal(getAuth())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(timesheetService).rejectTimesheet(
                anyString(),
                argThat(java.util.Map::isEmpty));
    }
}
