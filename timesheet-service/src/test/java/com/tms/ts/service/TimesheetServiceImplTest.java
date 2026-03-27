package com.tms.ts.service;

import com.tms.ts.client.AuthServiceClient;
import com.tms.common.exception.ResourceNotFoundException;
import com.tms.common.exception.UnauthorizedException;
import com.tms.common.util.IdGeneratorUtil;
import com.tms.ts.dto.TimesheetEntryRequest;
import com.tms.ts.dto.TimesheetEntryResponse;
import com.tms.ts.dto.TimesheetResponse;
import com.tms.ts.dto.TimesheetSubmitRequest;
import com.tms.ts.dto.TimesheetValidationResponse;
import com.tms.ts.entity.Project;
import com.tms.ts.entity.Timesheet;
import com.tms.ts.entity.TimesheetEntry;
import com.tms.ts.entity.TimesheetStatus;
import com.tms.ts.event.TimesheetSubmittedEvent;
import com.tms.ts.repository.ProjectRepository;
import com.tms.ts.repository.TimesheetEntryRepository;
import com.tms.ts.repository.TimesheetRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TimesheetServiceImplTest {

    @Mock
    private TimesheetRepository timesheetRepository;

    @Mock
    private TimesheetEntryRepository timesheetEntryRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private AuthServiceClient authServiceClient;

    @Mock
    private IdGeneratorUtil idGeneratorUtil;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private TimesheetServiceImpl timesheetService;

    private Timesheet testTimesheet;
    private Project testProject;
    private TimesheetEntry testEntry;
    private TimesheetEntryRequest addRequest;
    private final String EMPLOYEE_ID = "USR-123";
    private final String AUTHORIZATION = "Bearer test-token";
    private final LocalDate TODAY = LocalDate.now();
    private final LocalDate WEEK_START = TODAY.with(DayOfWeek.MONDAY);

    @BeforeEach
    void setUp() {
        testProject = new Project();
        testProject.setId("PRJ-101");
        testProject.setName("Test Project");
        testProject.setActive(true);

        testTimesheet = new Timesheet();
        testTimesheet.setId("TS-001");
        testTimesheet.setEmployeeId(EMPLOYEE_ID);
        testTimesheet.setWeekStart(WEEK_START);
        testTimesheet.setStatus(TimesheetStatus.DRAFT);

        testEntry = new TimesheetEntry();
        testEntry.setId("TE-001");
        testEntry.setTimesheet(testTimesheet);
        testEntry.setProjectId("PRJ-101");
        testEntry.setWorkDate(TODAY);
        testEntry.setHoursWorked(new BigDecimal("8.0"));

        addRequest = new TimesheetEntryRequest();
        addRequest.setProjectId("PRJ-101");
        addRequest.setWorkDate(TODAY);
        addRequest.setHoursWorked(new BigDecimal("8.0"));
        addRequest.setTaskSummary("Did some work");
    }

    // ==================== ADD ENTRY TESTS ====================

    @Nested
    @DisplayName("Add Entry Tests")
    class AddEntryTests {

        @Test
        @DisplayName("Should successfully add a new entry to a draft timesheet")
        void addEntry_Success() {
            when(projectRepository.findById("PRJ-101")).thenReturn(Optional.of(testProject));
            when(timesheetRepository.findByEmployeeIdAndWeekStart(EMPLOYEE_ID, WEEK_START))
                    .thenReturn(Optional.of(testTimesheet));
            when(timesheetEntryRepository.existsByTimesheetIdAndWorkDateAndProjectId("TS-001", TODAY, "PRJ-101"))
                    .thenReturn(false);
            when(timesheetEntryRepository.getTotalHoursForEmployeeAndDate(EMPLOYEE_ID, TODAY))
                    .thenReturn(BigDecimal.ZERO);
            when(idGeneratorUtil.generateId("TE")).thenReturn("TE-001");
            when(timesheetEntryRepository.save(any(TimesheetEntry.class))).thenReturn(testEntry);

            TimesheetEntryResponse response = timesheetService.addEntry(addRequest, EMPLOYEE_ID, AUTHORIZATION);

            assertNotNull(response);
            assertEquals("TE-001", response.getId());
            assertEquals("PRJ-101", response.getProjectId());
            assertEquals("Test Project", response.getProjectName());
            assertEquals(new BigDecimal("8.0"), response.getHoursWorked());
            verify(timesheetEntryRepository).save(any(TimesheetEntry.class));
        }

        @Test
        @DisplayName("Should fail when max hours exceeded")
        void addEntry_MaxHoursExceeded() {
            when(projectRepository.findById("PRJ-101")).thenReturn(Optional.of(testProject));
            when(timesheetRepository.findByEmployeeIdAndWeekStart(EMPLOYEE_ID, WEEK_START))
                    .thenReturn(Optional.of(testTimesheet));
            when(timesheetEntryRepository.existsByTimesheetIdAndWorkDateAndProjectId("TS-001", TODAY, "PRJ-101"))
                    .thenReturn(false);
            // Already worked 20 hours today, trying to add 8 more.
            when(timesheetEntryRepository.getTotalHoursForEmployeeAndDate(EMPLOYEE_ID, TODAY))
                    .thenReturn(new BigDecimal("20.0"));

            UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                    () -> timesheetService.addEntry(addRequest, EMPLOYEE_ID, AUTHORIZATION));

            assertTrue(ex.getMessage().contains("Daily hours cannot exceed 24"));
            verify(timesheetEntryRepository, never()).save(any(TimesheetEntry.class));
        }

        @Test
        @DisplayName("Should throw exception if project is not active")
        void addEntry_InactiveProject() {
            testProject.setActive(false);
            when(projectRepository.findById("PRJ-101")).thenReturn(Optional.of(testProject));

            UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                    () -> timesheetService.addEntry(addRequest, EMPLOYEE_ID, AUTHORIZATION));

            assertEquals("Project is not active", ex.getMessage());
            verify(timesheetEntryRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception if work date is in the future")
        void addEntry_FutureDate() {
            addRequest.setWorkDate(TODAY.plusDays(1));

            UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                    () -> timesheetService.addEntry(addRequest, EMPLOYEE_ID, AUTHORIZATION));

            assertEquals("Work date cannot be in the future", ex.getMessage());
        }

        @Test
        @DisplayName("Should throw exception if timesheet is already submitted")
        void addEntry_TimesheetSubmitted() {
            testTimesheet.setStatus(TimesheetStatus.SUBMITTED);
            when(projectRepository.findById("PRJ-101")).thenReturn(Optional.of(testProject));
            when(timesheetRepository.findByEmployeeIdAndWeekStart(EMPLOYEE_ID, WEEK_START))
                    .thenReturn(Optional.of(testTimesheet));

            UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                    () -> timesheetService.addEntry(addRequest, EMPLOYEE_ID, AUTHORIZATION));

            assertEquals("Cannot add entry to a submitted or approved timesheet", ex.getMessage());
        }

        @Test
        @DisplayName("Should create a draft timesheet when one does not exist")
        void addEntry_CreatesTimesheetWhenMissing() {
            when(projectRepository.findById("PRJ-101")).thenReturn(Optional.of(testProject));
            when(timesheetRepository.findByEmployeeIdAndWeekStart(EMPLOYEE_ID, WEEK_START))
                    .thenReturn(Optional.empty());
            when(authServiceClient.getManagerIdForEmployee(EMPLOYEE_ID, AUTHORIZATION)).thenReturn(null);
            when(idGeneratorUtil.generateId("TS")).thenReturn("TS-NEW");
            when(idGeneratorUtil.generateId("TE")).thenReturn("TE-001");
            when(timesheetRepository.save(any(Timesheet.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(timesheetEntryRepository.existsByTimesheetIdAndWorkDateAndProjectId("TS-NEW", TODAY, "PRJ-101"))
                    .thenReturn(false);
            when(timesheetEntryRepository.getTotalHoursForEmployeeAndDate(EMPLOYEE_ID, TODAY))
                    .thenReturn(BigDecimal.ZERO);
            when(timesheetEntryRepository.save(any(TimesheetEntry.class))).thenReturn(testEntry);

            TimesheetEntryResponse response = timesheetService.addEntry(addRequest, EMPLOYEE_ID, AUTHORIZATION);

            assertNotNull(response);
            verify(timesheetRepository).save(any(Timesheet.class));
            verify(authServiceClient).getManagerIdForEmployee(EMPLOYEE_ID, AUTHORIZATION);
        }
    }

    // ==================== UPDATE ENTRY TESTS ====================

    @Nested
    @DisplayName("Update Entry Tests")
    class UpdateEntryTests {

        @Test
        @DisplayName("Should successfully update an existing entry")
        void updateEntry_Success() {
            when(timesheetEntryRepository.findById("TE-001")).thenReturn(Optional.of(testEntry));
            when(projectRepository.findById("PRJ-101")).thenReturn(Optional.of(testProject));
            when(timesheetEntryRepository.getTotalHoursForEmployeeAndDate(EMPLOYEE_ID, TODAY))
                    .thenReturn(new BigDecimal("8.0"));
            when(timesheetEntryRepository.save(any(TimesheetEntry.class))).thenReturn(testEntry);

            TimesheetEntryResponse response = timesheetService.updateEntry("TE-001", addRequest, EMPLOYEE_ID);

            assertNotNull(response);
            assertEquals("Test Project", response.getProjectName());
            verify(timesheetEntryRepository).save(any(TimesheetEntry.class));
        }

        @Test
        @DisplayName("Should throw exception if trying to update another user's entry")
        void updateEntry_NotOwnEntry() {
            when(timesheetEntryRepository.findById("TE-001")).thenReturn(Optional.of(testEntry));

            UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                    () -> timesheetService.updateEntry("TE-001", addRequest, "OTHER-EMP"));

            assertEquals("You can only update your own entries", ex.getMessage());
            verify(timesheetEntryRepository, never()).save(any());
        }
    }

    // ==================== DELETE ENTRY TESTS ====================

    @Nested
    @DisplayName("Delete Entry Tests")
    class DeleteEntryTests {

        @Test
        @DisplayName("Should successfully delete an entry")
        void deleteEntry_Success() {
            when(timesheetEntryRepository.findById("TE-001")).thenReturn(Optional.of(testEntry));
            
            timesheetService.deleteEntry("TE-001", EMPLOYEE_ID);

            verify(timesheetEntryRepository).delete(testEntry);
        }
    }

    // ==================== GET & VALIDATE TIMESHEET TESTS ====================

    @Nested
    @DisplayName("Get Timesheet Tests")
    class GetTimesheetTests {

        @Test
        @DisplayName("Should fetch timesheet with mapped entries")
        void getTimesheet_Success() {
            testTimesheet.setEntries(List.of(testEntry));
            when(timesheetRepository.findByEmployeeIdAndWeekStart(EMPLOYEE_ID, WEEK_START))
                    .thenReturn(Optional.of(testTimesheet));
            when(projectRepository.findById("PRJ-101")).thenReturn(Optional.of(testProject));

            TimesheetResponse response = timesheetService.getTimesheet(WEEK_START, EMPLOYEE_ID);

            assertNotNull(response);
            assertEquals("TS-001", response.getId());
            assertEquals(1, response.getEntries().size());
            assertEquals("Test Project", response.getEntries().get(0).getProjectName());
        }

        @Test
        @DisplayName("Validate timesheet with missing days")
        void validateTimesheet_MissingDays() {
            // Only adding entry for TODAY
            testTimesheet.setEntries(List.of(testEntry));
            when(timesheetRepository.findByEmployeeIdAndWeekStart(EMPLOYEE_ID, WEEK_START))
                    .thenReturn(Optional.of(testTimesheet));

            TimesheetValidationResponse response = timesheetService.validateTimesheet(WEEK_START, EMPLOYEE_ID);

            assertFalse(response.isValid());
            // Since there's 5 weekdays and user only logged 1 day, there should be 4 errors
            assertEquals(4, response.getErrors().size());
        }
    }

    // ==================== SUBMIT TIMESHEET TESTS ====================

    @Nested
    @DisplayName("Submit Timesheet Tests")
    class SubmitTimesheetTests {

        @Test
        @DisplayName("Should submit valid timesheet and send RabbitMQ event")
        void submitTimesheet_Success() {
            // Set up 5 entries for Monday-Friday to pass validation
            TimesheetEntry e1 = new TimesheetEntry(); e1.setWorkDate(WEEK_START); e1.setHoursWorked(new BigDecimal("8"));
            TimesheetEntry e2 = new TimesheetEntry(); e2.setWorkDate(WEEK_START.plusDays(1)); e2.setHoursWorked(new BigDecimal("8"));
            TimesheetEntry e3 = new TimesheetEntry(); e3.setWorkDate(WEEK_START.plusDays(2)); e3.setHoursWorked(new BigDecimal("8"));
            TimesheetEntry e4 = new TimesheetEntry(); e4.setWorkDate(WEEK_START.plusDays(3)); e4.setHoursWorked(new BigDecimal("8"));
            TimesheetEntry e5 = new TimesheetEntry(); e5.setWorkDate(WEEK_START.plusDays(4)); e5.setHoursWorked(new BigDecimal("8"));
            
            // Re-mocking the entries with valid IDs and projects to satisfy mapToEntryResponse downstream
            e1.setProjectId("PRJ-101"); e1.setTimesheet(testTimesheet);
            e2.setProjectId("PRJ-101"); e2.setTimesheet(testTimesheet);
            e3.setProjectId("PRJ-101"); e3.setTimesheet(testTimesheet);
            e4.setProjectId("PRJ-101"); e4.setTimesheet(testTimesheet);
            e5.setProjectId("PRJ-101"); e5.setTimesheet(testTimesheet);

            testTimesheet.setEntries(List.of(e1, e2, e3, e4, e5));
            TimesheetSubmitRequest submitRequest = new TimesheetSubmitRequest();
            submitRequest.setWeekStart(WEEK_START);
            submitRequest.setComments("Week done");

            when(timesheetRepository.findByEmployeeIdAndWeekStart(EMPLOYEE_ID, WEEK_START))
                    .thenReturn(Optional.of(testTimesheet));
            when(timesheetRepository.save(any(Timesheet.class))).thenReturn(testTimesheet);
            when(projectRepository.findById("PRJ-101")).thenReturn(Optional.of(testProject));
            when(authServiceClient.getManagerIdForEmployee(EMPLOYEE_ID, AUTHORIZATION)).thenReturn("USR-MGR001");

            TimesheetResponse response = timesheetService.submitTimesheet(submitRequest, EMPLOYEE_ID, AUTHORIZATION);

            assertEquals(TimesheetStatus.SUBMITTED, response.getStatus());
            verify(timesheetRepository).save(testTimesheet);
            verify(rabbitTemplate).convertAndSend(eq("timesheet.exchange"), eq("timesheet.submitted"), any(TimesheetSubmittedEvent.class));
        }

        @Test
        @DisplayName("Should throw exception if validation fails while submitting (TC-03)")
        void submitTimesheet_MissingDays() {
            // Only 1 entry leads to validation failure
            testTimesheet.setEntries(List.of(testEntry));
            TimesheetSubmitRequest submitRequest = new TimesheetSubmitRequest();
            submitRequest.setWeekStart(WEEK_START);

            when(timesheetRepository.findByEmployeeIdAndWeekStart(EMPLOYEE_ID, WEEK_START))
                    .thenReturn(Optional.of(testTimesheet));

            UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                    () -> timesheetService.submitTimesheet(submitRequest, EMPLOYEE_ID, AUTHORIZATION));

            assertTrue(ex.getMessage().contains("Cannot submit timesheet"));
            verify(timesheetRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception if timesheet has no entries")
        void submitTimesheet_Empty() {
            testTimesheet.setEntries(Collections.emptyList());
            TimesheetSubmitRequest submitRequest = new TimesheetSubmitRequest();
            submitRequest.setWeekStart(WEEK_START);

            when(timesheetRepository.findByEmployeeIdAndWeekStart(EMPLOYEE_ID, WEEK_START))
                    .thenReturn(Optional.of(testTimesheet));

            UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                    () -> timesheetService.submitTimesheet(submitRequest, EMPLOYEE_ID, AUTHORIZATION));

            assertEquals("Cannot submit an empty timesheet", ex.getMessage());
            verify(timesheetRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception if no manager is assigned during submission")
        void submitTimesheet_NoManagerAssigned() {
            TimesheetEntry e1 = new TimesheetEntry(); e1.setWorkDate(WEEK_START); e1.setHoursWorked(new BigDecimal("8"));
            TimesheetEntry e2 = new TimesheetEntry(); e2.setWorkDate(WEEK_START.plusDays(1)); e2.setHoursWorked(new BigDecimal("8"));
            TimesheetEntry e3 = new TimesheetEntry(); e3.setWorkDate(WEEK_START.plusDays(2)); e3.setHoursWorked(new BigDecimal("8"));
            TimesheetEntry e4 = new TimesheetEntry(); e4.setWorkDate(WEEK_START.plusDays(3)); e4.setHoursWorked(new BigDecimal("8"));
            TimesheetEntry e5 = new TimesheetEntry(); e5.setWorkDate(WEEK_START.plusDays(4)); e5.setHoursWorked(new BigDecimal("8"));

            testTimesheet.setEntries(List.of(e1, e2, e3, e4, e5));
            TimesheetSubmitRequest submitRequest = new TimesheetSubmitRequest();
            submitRequest.setWeekStart(WEEK_START);

            when(timesheetRepository.findByEmployeeIdAndWeekStart(EMPLOYEE_ID, WEEK_START))
                    .thenReturn(Optional.of(testTimesheet));
            when(authServiceClient.getManagerIdForEmployee(EMPLOYEE_ID, AUTHORIZATION)).thenReturn(" ");

            UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                    () -> timesheetService.submitTimesheet(submitRequest, EMPLOYEE_ID, AUTHORIZATION));

            assertEquals("Cannot submit timesheet: No manager assigned to approve", ex.getMessage());
            verify(timesheetRepository, never()).save(any());
            verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(TimesheetSubmittedEvent.class));
        }
    }

    @Nested
    @DisplayName("Approval Decision Tests")
    class ApprovalDecisionTests {

        @Test
        @DisplayName("Should approve timesheet and persist approval comment")
        void approveTimesheet_Success() {
            when(timesheetRepository.findById("TS-001")).thenReturn(Optional.of(testTimesheet));
            when(timesheetRepository.save(any(Timesheet.class))).thenAnswer(invocation -> invocation.getArgument(0));

            timesheetService.approveTimesheet("TS-001", Map.of("comments", "Looks good"));

            assertEquals(TimesheetStatus.APPROVED, testTimesheet.getStatus());
            assertEquals("Looks good", testTimesheet.getComments());
            assertNotNull(testTimesheet.getApprovedAt());
            verify(timesheetRepository).save(testTimesheet);
        }

        @Test
        @DisplayName("Should reject timesheet and persist rejection reason")
        void rejectTimesheet_Success() {
            when(timesheetRepository.findById("TS-001")).thenReturn(Optional.of(testTimesheet));
            when(timesheetRepository.save(any(Timesheet.class))).thenAnswer(invocation -> invocation.getArgument(0));

            timesheetService.rejectTimesheet("TS-001", Map.of("comments", "Missing details"));

            assertEquals(TimesheetStatus.REJECTED, testTimesheet.getStatus());
            assertEquals("Missing details", testTimesheet.getRejectionReason());
            verify(timesheetRepository).save(testTimesheet);
        }
    }
}
