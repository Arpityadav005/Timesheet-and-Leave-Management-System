package com.tms.ls.service;

import com.tms.common.exception.ResourceNotFoundException;
import com.tms.ls.client.AuthServiceClient;
import com.tms.ls.dto.LeaveRequestDto;
import com.tms.ls.dto.LeaveRequestedEvent;
import com.tms.ls.dto.LeaveResponse;
import com.tms.ls.entity.LeaveBalance;
import com.tms.ls.entity.LeaveRequest;
import com.tms.ls.entity.LeaveStatus;
import com.tms.ls.entity.LeaveType;
import com.tms.ls.repository.LeaveBalanceRepository;
import com.tms.ls.repository.LeavePolicyRepository;
import com.tms.ls.repository.LeaveRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeaveServiceImplTest {

    @Mock
    private LeaveBalanceRepository balanceRepository;

    @Mock
    private LeavePolicyRepository leavePolicyRepository;

    @Mock
    private LeaveRequestRepository requestRepository;

    @Mock
    private AuthServiceClient authServiceClient;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private HolidayService holidayService;

    @InjectMocks
    private LeaveServiceImpl service;

    private LeaveRequestDto requestDto;
    private LeaveBalance balance;

    @BeforeEach
    void setUp() {
        requestDto = new LeaveRequestDto();
        requestDto.setLeaveType(LeaveType.CASUAL);
        requestDto.setStartDate(LocalDate.of(2026, 3, 30));
        requestDto.setEndDate(LocalDate.of(2026, 4, 1));
        requestDto.setReason("Vacation");

        balance = new LeaveBalance("emp-1", LeaveType.CASUAL, new BigDecimal("10"));
    }

    @Test
    void getTeamCalendar_ReturnsLeaveAndHolidayData() {
        LeaveRequest request = leaveRequest("leave-1", LeaveStatus.SUBMITTED);
        request.setCreatedAt(LocalDateTime.of(2026, 3, 1, 10, 0));
        when(requestRepository.findByApproverId("mgr-1")).thenReturn(List.of(request));
        when(holidayService.getAllHolidays()).thenReturn(List.of());

        var response = service.getTeamCalendar("mgr-1");

        assertEquals(1, response.getTeamLeaves().size());
        assertEquals("leave-1", response.getTeamLeaves().get(0).getId());
        assertEquals(List.of(), response.getHolidays());
    }

    @Test
    void getBalances_ReturnsEmptyListWhenNoneExist() {
        when(balanceRepository.findByEmployeeId("emp-1")).thenReturn(List.of());

        assertTrue(service.getBalances("emp-1").isEmpty());
    }

    @Test
    void getBalances_ReturnsMappedBalances() {
        when(balanceRepository.findByEmployeeId("emp-1")).thenReturn(List.of(balance));

        var responses = service.getBalances("emp-1");

        assertEquals(1, responses.size());
        assertEquals("emp-1", responses.get(0).getEmployeeId());
        assertEquals(LeaveType.CASUAL, responses.get(0).getLeaveType());
    }

    @Nested
    class InitializeBalanceTests {

        @Test
        void initializeBalances_CreatesBalancesFromActivePolicies() {
            var policy = new com.tms.ls.entity.LeavePolicy();
            policy.setLeaveType(LeaveType.SICK);
            policy.setDaysAllowed(new BigDecimal("12"));

            when(balanceRepository.findByEmployeeId("emp-1")).thenReturn(List.of());
            when(leavePolicyRepository.findByActiveTrue()).thenReturn(List.of(policy));

            service.initializeBalances("emp-1");

            verify(balanceRepository).save(any(LeaveBalance.class));
        }

        @Test
        void initializeBalances_DoesNothingWhenBalancesAlreadyExist() {
            when(balanceRepository.findByEmployeeId("emp-1")).thenReturn(List.of(balance));

            service.initializeBalances("emp-1");

            verify(leavePolicyRepository, never()).findByActiveTrue();
        }

        @Test
        void initializeBalances_ThrowsWhenNoPoliciesExist() {
            when(balanceRepository.findByEmployeeId("emp-1")).thenReturn(List.of());
            when(leavePolicyRepository.findByActiveTrue()).thenReturn(List.of());

            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> service.initializeBalances("emp-1"));

            assertEquals("No active leave policies found to initialize balances", exception.getMessage());
        }
    }

    @Test
    void getMyRequests_ReturnsMappedResponses() {
        LeaveRequest request = leaveRequest("leave-1", LeaveStatus.SUBMITTED);
        when(requestRepository.findByEmployeeIdOrderByStartDateDesc("emp-1")).thenReturn(List.of(request));

        List<LeaveResponse> responses = service.getMyRequests("emp-1");

        assertEquals(1, responses.size());
        assertEquals("leave-1", responses.get(0).getId());
        assertEquals(LeaveStatus.SUBMITTED, responses.get(0).getStatus());
    }

    @Nested
    class RequestLeaveTests {

        @Test
        void requestLeave_Success() {
            when(requestRepository.findOverlappingRequests("emp-1", requestDto.getStartDate(), requestDto.getEndDate()))
                    .thenReturn(List.of());
            when(balanceRepository.findByEmployeeIdAndLeaveType("emp-1", LeaveType.CASUAL))
                    .thenReturn(Optional.of(balance));
            when(authServiceClient.getManagerIdForEmployee("emp-1", "Bearer token"))
                    .thenReturn("mgr-1");
            when(requestRepository.save(any(LeaveRequest.class))).thenAnswer(invocation -> {
                LeaveRequest saved = invocation.getArgument(0);
                saved.setId("leave-1");
                return saved;
            });

            LeaveResponse response = service.requestLeave(requestDto, "emp-1", "Bearer token");

            assertEquals("leave-1", response.getId());
            assertEquals(LeaveStatus.SUBMITTED, response.getStatus());
            assertEquals("mgr-1", response.getApproverId());
            assertEquals(new BigDecimal("3"), balance.getPending());
            assertEquals(BigDecimal.ZERO, balance.getUsed());

            ArgumentCaptor<LeaveRequestedEvent> eventCaptor = ArgumentCaptor.forClass(LeaveRequestedEvent.class);
            verify(rabbitTemplate).convertAndSend(
                    anyString(),
                    anyString(),
                    eventCaptor.capture());
            assertEquals("leave-1", eventCaptor.getValue().getRequestId());
            assertEquals("mgr-1", eventCaptor.getValue().getApproverId());
        }

        @Test
        void requestLeave_AllowsUnpaidLeaveWithoutAvailableBalance() {
            requestDto.setLeaveType(LeaveType.UNPAID);
            balance = new LeaveBalance("emp-1", LeaveType.UNPAID, BigDecimal.ZERO);

            when(requestRepository.findOverlappingRequests("emp-1", requestDto.getStartDate(), requestDto.getEndDate()))
                    .thenReturn(List.of());
            when(balanceRepository.findByEmployeeIdAndLeaveType("emp-1", LeaveType.UNPAID))
                    .thenReturn(Optional.of(balance));
            when(authServiceClient.getManagerIdForEmployee("emp-1", "Bearer token"))
                    .thenReturn("mgr-1");
            when(requestRepository.save(any(LeaveRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

            service.requestLeave(requestDto, "emp-1", "Bearer token");

            assertEquals(new BigDecimal("3"), balance.getPending());
            verify(rabbitTemplate).convertAndSend(anyString(), anyString(), any(LeaveRequestedEvent.class));
        }

        @Test
        void requestLeave_ThrowsWhenManagerMissing() {
            when(requestRepository.findOverlappingRequests("emp-1", requestDto.getStartDate(), requestDto.getEndDate()))
                    .thenReturn(List.of());
            when(balanceRepository.findByEmployeeIdAndLeaveType("emp-1", LeaveType.CASUAL))
                    .thenReturn(Optional.of(balance));
            when(authServiceClient.getManagerIdForEmployee("emp-1", "Bearer token"))
                    .thenReturn(" ");

            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> service.requestLeave(requestDto, "emp-1", "Bearer token"));

            assertTrue(exception.getMessage().contains("No manager assigned"));
            verify(requestRepository, never()).save(any(LeaveRequest.class));
            verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(LeaveRequestedEvent.class));
        }

        @Test
        void requestLeave_ThrowsWhenStartDateAfterEndDate() {
            requestDto.setStartDate(LocalDate.of(2026, 4, 2));
            requestDto.setEndDate(LocalDate.of(2026, 4, 1));

            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> service.requestLeave(requestDto, "emp-1", "Bearer token"));

            assertEquals("Start date cannot be after end date", exception.getMessage());
        }

        @Test
        void requestLeave_ThrowsWhenOverlappingRequestExists() {
            when(requestRepository.findOverlappingRequests("emp-1", requestDto.getStartDate(), requestDto.getEndDate()))
                    .thenReturn(List.of(new LeaveRequest()));

            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> service.requestLeave(requestDto, "emp-1", "Bearer token"));

            assertEquals("You already have an active leave request during this period", exception.getMessage());
        }

        @Test
        void requestLeave_ThrowsWhenNoWorkingDaysExist() {
            requestDto.setStartDate(LocalDate.of(2026, 4, 4));
            requestDto.setEndDate(LocalDate.of(2026, 4, 5));

            when(requestRepository.findOverlappingRequests("emp-1", requestDto.getStartDate(), requestDto.getEndDate()))
                    .thenReturn(List.of());

            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> service.requestLeave(requestDto, "emp-1", "Bearer token"));

            assertEquals("Requested period does not contain any working days", exception.getMessage());
        }

        @Test
        void requestLeave_ThrowsWhenBalanceMissing() {
            when(requestRepository.findOverlappingRequests("emp-1", requestDto.getStartDate(), requestDto.getEndDate()))
                    .thenReturn(List.of());
            when(balanceRepository.findByEmployeeIdAndLeaveType("emp-1", LeaveType.CASUAL))
                    .thenReturn(Optional.empty());

            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> service.requestLeave(requestDto, "emp-1", "Bearer token"));

            assertEquals("Leave balance not found for type: CASUAL", exception.getMessage());
        }

        @Test
        void requestLeave_ThrowsWhenInsufficientBalance() {
            balance.setTotalAllowed(new BigDecimal("2"));

            when(requestRepository.findOverlappingRequests("emp-1", requestDto.getStartDate(), requestDto.getEndDate()))
                    .thenReturn(List.of());
            when(balanceRepository.findByEmployeeIdAndLeaveType("emp-1", LeaveType.CASUAL))
                    .thenReturn(Optional.of(balance));

            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> service.requestLeave(requestDto, "emp-1", "Bearer token"));

            assertTrue(exception.getMessage().contains("Insufficient leave balance"));
        }
    }

    @Nested
    class ApprovalTests {

        @Test
        void approveLeave_UpdatesBalanceAndComment() {
            LeaveRequest request = leaveRequest("leave-1", LeaveStatus.SUBMITTED);
            balance.setPending(new BigDecimal("3"));

            when(requestRepository.findById("leave-1")).thenReturn(Optional.of(request));
            when(balanceRepository.findByEmployeeIdAndLeaveType("emp-1", LeaveType.CASUAL))
                    .thenReturn(Optional.of(balance));

            service.approveLeave("leave-1", Map.of("comments", "Approved"));

            assertEquals(LeaveStatus.APPROVED, request.getStatus());
            assertTrue(request.getReason().contains("Approved"));
            assertEquals(BigDecimal.ZERO, balance.getPending());
            assertEquals(new BigDecimal("3"), balance.getUsed());
            verify(balanceRepository).save(balance);
        }

        @Test
        void rejectLeave_UpdatesBalanceAndComment() {
            LeaveRequest request = leaveRequest("leave-1", LeaveStatus.SUBMITTED);
            balance.setPending(new BigDecimal("3"));

            when(requestRepository.findById("leave-1")).thenReturn(Optional.of(request));
            when(balanceRepository.findByEmployeeIdAndLeaveType("emp-1", LeaveType.CASUAL))
                    .thenReturn(Optional.of(balance));

            service.rejectLeave("leave-1", Map.of("comments", "Rejected"));

            assertEquals(LeaveStatus.REJECTED, request.getStatus());
            assertTrue(request.getReason().contains("Rejected"));
            assertEquals(BigDecimal.ZERO, balance.getPending());
            assertEquals(BigDecimal.ZERO, balance.getUsed());
            verify(balanceRepository).save(balance);
        }

        @Test
        void approveLeave_ThrowsWhenRequestMissing() {
            when(requestRepository.findById("missing")).thenReturn(Optional.empty());

            ResourceNotFoundException exception = assertThrows(
                    ResourceNotFoundException.class,
                    () -> service.approveLeave("missing", Map.of()));

            assertEquals("Leave request not found", exception.getMessage());
        }

        @Test
        void approveLeave_ThrowsWhenBalanceMissing() {
            LeaveRequest request = leaveRequest("leave-1", LeaveStatus.SUBMITTED);
            when(requestRepository.findById("leave-1")).thenReturn(Optional.of(request));
            when(balanceRepository.findByEmployeeIdAndLeaveType("emp-1", LeaveType.CASUAL))
                    .thenReturn(Optional.empty());

            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> service.approveLeave("leave-1", Map.of()));

            assertEquals("Leave balance not found", exception.getMessage());
        }

        @Test
        void rejectLeave_ThrowsWhenBalanceMissing() {
            LeaveRequest request = leaveRequest("leave-1", LeaveStatus.SUBMITTED);
            when(requestRepository.findById("leave-1")).thenReturn(Optional.of(request));
            when(balanceRepository.findByEmployeeIdAndLeaveType("emp-1", LeaveType.CASUAL))
                    .thenReturn(Optional.empty());

            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> service.rejectLeave("leave-1", Map.of()));

            assertEquals("Leave balance not found", exception.getMessage());
        }
    }

    @Nested
    class CancelTests {

        @Test
        void cancelLeaveRequest_RevertsPendingForSubmittedLeave() {
            LeaveRequest request = leaveRequest("leave-1", LeaveStatus.SUBMITTED);
            balance.setPending(new BigDecimal("3"));

            when(requestRepository.findById("leave-1")).thenReturn(Optional.of(request));
            when(balanceRepository.findByEmployeeIdAndLeaveType("emp-1", LeaveType.CASUAL))
                    .thenReturn(Optional.of(balance));

            service.cancelLeaveRequest("leave-1", "emp-1");

            assertEquals(LeaveStatus.CANCELLED, request.getStatus());
            assertEquals(BigDecimal.ZERO, balance.getPending());
            assertEquals(BigDecimal.ZERO, balance.getUsed());
        }

        @Test
        void cancelLeaveRequest_RevertsUsedForApprovedLeave() {
            LeaveRequest request = leaveRequest("leave-1", LeaveStatus.APPROVED);
            balance.setUsed(new BigDecimal("3"));

            when(requestRepository.findById("leave-1")).thenReturn(Optional.of(request));
            when(balanceRepository.findByEmployeeIdAndLeaveType("emp-1", LeaveType.CASUAL))
                    .thenReturn(Optional.of(balance));

            service.cancelLeaveRequest("leave-1", "emp-1");

            assertEquals(LeaveStatus.CANCELLED, request.getStatus());
            assertEquals(BigDecimal.ZERO, balance.getUsed());
        }

        @Test
        void cancelLeaveRequest_ThrowsWhenEmployeeDoesNotOwnRequest() {
            LeaveRequest request = leaveRequest("leave-1", LeaveStatus.SUBMITTED);
            request.setEmployeeId("other-emp");
            when(requestRepository.findById("leave-1")).thenReturn(Optional.of(request));

            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> service.cancelLeaveRequest("leave-1", "emp-1"));

            assertEquals("Cannot cancel someone else's leave request", exception.getMessage());
        }

        @Test
        void cancelLeaveRequest_ThrowsWhenAlreadyRejected() {
            LeaveRequest request = leaveRequest("leave-1", LeaveStatus.REJECTED);
            when(requestRepository.findById("leave-1")).thenReturn(Optional.of(request));

            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> service.cancelLeaveRequest("leave-1", "emp-1"));

            assertEquals("Leave request is already REJECTED", exception.getMessage());
        }

        @Test
        void cancelLeaveRequest_ThrowsWhenBalanceMissing() {
            LeaveRequest request = leaveRequest("leave-1", LeaveStatus.SUBMITTED);
            when(requestRepository.findById("leave-1")).thenReturn(Optional.of(request));
            when(balanceRepository.findByEmployeeIdAndLeaveType("emp-1", LeaveType.CASUAL))
                    .thenReturn(Optional.empty());

            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> service.cancelLeaveRequest("leave-1", "emp-1"));

            assertEquals("Leave balance not found", exception.getMessage());
        }
    }

    private LeaveRequest leaveRequest(String id, LeaveStatus status) {
        LeaveRequest request = new LeaveRequest();
        request.setId(id);
        request.setEmployeeId("emp-1");
        request.setLeaveType(LeaveType.CASUAL);
        request.setStartDate(requestDto.getStartDate());
        request.setEndDate(requestDto.getEndDate());
        request.setReason("Vacation");
        request.setStatus(status);
        request.setApproverId("mgr-1");
        return request;
    }
}
