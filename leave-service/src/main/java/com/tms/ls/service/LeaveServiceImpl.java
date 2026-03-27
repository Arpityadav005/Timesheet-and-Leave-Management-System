package com.tms.ls.service;

import com.tms.common.exception.ResourceNotFoundException;
import com.tms.ls.client.AuthServiceClient;
import com.tms.ls.dto.LeaveBalanceResponse;
import com.tms.ls.dto.LeaveRequestDto;
import com.tms.ls.dto.LeaveRequestedEvent;
import com.tms.ls.dto.LeaveResponse;
import com.tms.ls.dto.TeamCalendarResponse;
import com.tms.ls.entity.LeaveBalance;
import com.tms.ls.entity.LeavePolicy;
import com.tms.ls.entity.LeaveRequest;
import com.tms.ls.entity.LeaveStatus;
import com.tms.ls.entity.LeaveType;
import com.tms.ls.repository.LeaveBalanceRepository;
import com.tms.ls.repository.LeavePolicyRepository;
import com.tms.ls.repository.LeaveRequestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class LeaveServiceImpl implements LeaveService {
    private static final Logger log = LoggerFactory.getLogger(LeaveServiceImpl.class);

    private final LeaveBalanceRepository balanceRepository;
    private final LeavePolicyRepository leavePolicyRepository;
    private final LeaveRequestRepository requestRepository;
    private final AuthServiceClient authServiceClient;
    private final RabbitTemplate rabbitTemplate;
    private final HolidayService holidayService;

    public LeaveServiceImpl(LeaveBalanceRepository balanceRepository,
                            LeavePolicyRepository leavePolicyRepository,
                            LeaveRequestRepository requestRepository,
                            AuthServiceClient authServiceClient,
                            RabbitTemplate rabbitTemplate,
                            HolidayService holidayService) {
        this.balanceRepository = balanceRepository;
        this.leavePolicyRepository = leavePolicyRepository;
        this.requestRepository = requestRepository;
        this.authServiceClient = authServiceClient;
        this.rabbitTemplate = rabbitTemplate;
        this.holidayService = holidayService;
    }

    @Override
    @Transactional(readOnly = true)
    public TeamCalendarResponse getTeamCalendar(String managerId) {
        log.debug("Loading team calendar for managerId={}", managerId);
        List<LeaveResponse> teamLeaves = requestRepository.findByApproverId(managerId)
                .stream().map(this::mapReqToResponse).collect(Collectors.toList());
        return new TeamCalendarResponse(teamLeaves, holidayService.getAllHolidays());
    }

    @Override
    @Transactional(readOnly = true)
    public List<LeaveBalanceResponse> getBalances(String employeeId) {
        log.debug("Loading leave balances for employeeId={}", employeeId);
        List<LeaveBalance> balances = balanceRepository.findByEmployeeId(employeeId);
        if (balances.isEmpty()) {
            return List.of();
        }
        return balances.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void initializeBalances(String employeeId) {
        log.info("Initializing leave balances for employeeId={}", employeeId);
        List<LeaveBalance> existing = balanceRepository.findByEmployeeId(employeeId);
        if (existing.isEmpty()) {
            List<LeavePolicy> activePolicies = leavePolicyRepository.findByActiveTrue();
            if (activePolicies.isEmpty()) {
                log.warn("Leave balance initialization failed for employeeId={} because no active policies were found", employeeId);
                throw new IllegalArgumentException("No active leave policies found to initialize balances");
            }

            activePolicies.forEach(policy ->
                    balanceRepository.save(new LeaveBalance(
                            employeeId,
                            policy.getLeaveType(),
                            policy.getDaysAllowed()
                    ))
            );
            log.info("Leave balances initialized for employeeId={} using {} active policies", employeeId, activePolicies.size());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<LeaveResponse> getMyRequests(String employeeId) {
        log.debug("Loading leave requests for employeeId={}", employeeId);
        return requestRepository.findByEmployeeIdOrderByStartDateDesc(employeeId)
                .stream().map(this::mapReqToResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public LeaveResponse requestLeave(LeaveRequestDto requestDto, String employeeId, String authorization) {
        log.info("Submitting leave request for employeeId={} leaveType={} startDate={} endDate={}", employeeId, requestDto.getLeaveType(), requestDto.getStartDate(), requestDto.getEndDate());
        if (requestDto.getStartDate().isAfter(requestDto.getEndDate())) {
            log.warn("Leave request rejected for employeeId={} because startDate={} is after endDate={}", employeeId, requestDto.getStartDate(), requestDto.getEndDate());
            throw new IllegalArgumentException("Start date cannot be after end date");
        }

        List<LeaveRequest> overlaps = requestRepository.findOverlappingRequests(employeeId, requestDto.getStartDate(), requestDto.getEndDate());
        if (!overlaps.isEmpty()) {
            log.warn("Leave request rejected for employeeId={} because overlapping requests were found", employeeId);
            throw new IllegalArgumentException("You already have an active leave request during this period");
        }

        BigDecimal requestedDays = calculateWorkingDays(requestDto.getStartDate(), requestDto.getEndDate());
        if (requestedDays.compareTo(BigDecimal.ZERO) == 0) {
            log.warn("Leave request rejected for employeeId={} because requested period had no working days", employeeId);
            throw new IllegalArgumentException("Requested period does not contain any working days");
        }

        LeaveBalance balance = balanceRepository.findByEmployeeIdAndLeaveType(employeeId, requestDto.getLeaveType())
                .orElseThrow(() -> new IllegalArgumentException("Leave balance not found for type: " + requestDto.getLeaveType()));

        BigDecimal available = calculateAvailableBalance(balance);
        
        if (available.compareTo(requestedDays) < 0 && requestDto.getLeaveType() != LeaveType.UNPAID) {
            log.warn("Leave request rejected for employeeId={} because requestedDays={} exceeded available={}", employeeId, requestedDays, available);
            throw new IllegalArgumentException("Insufficient leave balance. requested: " + requestedDays + ", available: " + available);
        }

        String approverId = authServiceClient.getManagerIdForEmployee(employeeId, authorization);
        if (approverId == null || approverId.isBlank()) {
            log.warn("Leave request rejected for employeeId={} because no manager was assigned", employeeId);
            throw new IllegalArgumentException("Cannot submit leave: No manager assigned to approve");
        }

        LeaveRequest request = new LeaveRequest();
        request.setEmployeeId(employeeId);
        request.setLeaveType(requestDto.getLeaveType());
        request.setStartDate(requestDto.getStartDate());
        request.setEndDate(requestDto.getEndDate());
        request.setReason(requestDto.getReason());
        request.setStatus(LeaveStatus.SUBMITTED);
        request.setApproverId(approverId);
        
        request = requestRepository.save(request);

        updateBalance(balance, requestedDays, BigDecimal.ZERO);
        balanceRepository.save(balance);

        LeaveRequestedEvent event = new LeaveRequestedEvent(
                request.getId(),
                employeeId,
                approverId,
                request.getLeaveType().name(),
                request.getStartDate(),
                request.getEndDate()
        );
        rabbitTemplate.convertAndSend(
                com.tms.ls.config.RabbitMQConfig.EXCHANGE,
                com.tms.ls.config.RabbitMQConfig.LEAVE_ROUTING_KEY,
                event);
        log.info("Leave request submitted requestId={} employeeId={} approverId={}", request.getId(), employeeId, approverId);

        return mapReqToResponse(request);
    }

    @Override
    @Transactional
    public void approveLeave(String id, Map<String, String> comments) {
        log.info("Approving leave request id={}", id);
        LeaveRequest request = requestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Leave request not found"));
        request.setStatus(LeaveStatus.APPROVED);
        appendApproverComment(request, comments);
        requestRepository.save(request);
        
        LeaveBalance balance = balanceRepository.findByEmployeeIdAndLeaveType(request.getEmployeeId(), request.getLeaveType())
                .orElseThrow(() -> new IllegalArgumentException("Leave balance not found"));
        BigDecimal requestedDays = calculateWorkingDays(request.getStartDate(), request.getEndDate());
        updateBalance(balance, requestedDays.negate(), requestedDays);
        balanceRepository.save(balance);
        log.info("Leave request approved id={} employeeId={} requestedDays={}", id, request.getEmployeeId(), requestedDays);
    }

    @Override
    @Transactional
    public void rejectLeave(String id, Map<String, String> comments) {
        log.info("Rejecting leave request id={}", id);
        LeaveRequest request = requestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Leave request not found"));
        request.setStatus(LeaveStatus.REJECTED);
        appendApproverComment(request, comments);
        requestRepository.save(request);
        
        LeaveBalance balance = balanceRepository.findByEmployeeIdAndLeaveType(request.getEmployeeId(), request.getLeaveType())
                .orElseThrow(() -> new IllegalArgumentException("Leave balance not found"));
        BigDecimal requestedDays = calculateWorkingDays(request.getStartDate(), request.getEndDate());
        updateBalance(balance, requestedDays.negate(), BigDecimal.ZERO);
        balanceRepository.save(balance);
        log.info("Leave request rejected id={} employeeId={} requestedDays={}", id, request.getEmployeeId(), requestedDays);
    }

    @Override
    @Transactional
    public void cancelLeaveRequest(String id, String employeeId) {
        log.info("Cancelling leave request id={} employeeId={}", id, employeeId);
        LeaveRequest request = requestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Leave request not found"));
        
        if (!request.getEmployeeId().equals(employeeId)) {
            log.warn("Leave cancellation denied for requestId={} employeeId={} ownerId={}", id, employeeId, request.getEmployeeId());
            throw new IllegalArgumentException("Cannot cancel someone else's leave request");
        }

        if (request.getStatus() == LeaveStatus.CANCELLED || request.getStatus() == LeaveStatus.REJECTED) {
            log.warn("Leave cancellation rejected for requestId={} because status={}", id, request.getStatus());
            throw new IllegalArgumentException("Leave request is already " + request.getStatus());
        }

        LeaveStatus oldStatus = request.getStatus();
        request.setStatus(LeaveStatus.CANCELLED);
        requestRepository.save(request);

        LeaveBalance balance = balanceRepository.findByEmployeeIdAndLeaveType(employeeId, request.getLeaveType())
                .orElseThrow(() -> new IllegalArgumentException("Leave balance not found"));
        
        BigDecimal requestedDays = calculateWorkingDays(request.getStartDate(), request.getEndDate());
        
        if (oldStatus == LeaveStatus.SUBMITTED) {
            updateBalance(balance, requestedDays.negate(), BigDecimal.ZERO);
        } else if (oldStatus == LeaveStatus.APPROVED) {
            updateBalance(balance, BigDecimal.ZERO, requestedDays.negate());
        }
        
        balanceRepository.save(balance);
        log.info("Leave request cancelled id={} employeeId={} previousStatus={}", id, employeeId, oldStatus);
    }

    private BigDecimal calculateAvailableBalance(LeaveBalance balance) {
        return balance.getTotalAllowed().subtract(balance.getUsed()).subtract(balance.getPending());
    }

    private void appendApproverComment(LeaveRequest request, Map<String, String> comments) {
        String comment = getComment(comments);
        if (comment != null) {
            request.setReason(request.getReason() + " [Approver Comment: " + comment + "]");
        }
    }

    private String getComment(Map<String, String> comments) {
        if (comments == null) {
            return null;
        }
        return comments.get("comments");
    }

    private void updateBalance(LeaveBalance balance, BigDecimal pendingDelta, BigDecimal usedDelta) {
        balance.setPending(balance.getPending().add(pendingDelta));
        balance.setUsed(balance.getUsed().add(usedDelta));
    }

    private BigDecimal calculateWorkingDays(LocalDate start, LocalDate end) {
        long days = 0;
        LocalDate current = start;
        while (!current.isAfter(end)) {
            if (current.getDayOfWeek() != DayOfWeek.SATURDAY && current.getDayOfWeek() != DayOfWeek.SUNDAY) {
                days++;
            }
            current = current.plusDays(1);
        }
        return new BigDecimal(days);
    }

    private LeaveBalanceResponse mapToResponse(LeaveBalance entity) {
        LeaveBalanceResponse resp = new LeaveBalanceResponse();
        resp.setId(entity.getId());
        resp.setEmployeeId(entity.getEmployeeId());
        resp.setLeaveType(entity.getLeaveType());
        resp.setTotalAllowed(entity.getTotalAllowed());
        resp.setUsed(entity.getUsed());
        resp.setPending(entity.getPending());
        return resp;
    }

    private LeaveResponse mapReqToResponse(LeaveRequest entity) {
        LeaveResponse resp = new LeaveResponse();
        resp.setId(entity.getId());
        resp.setEmployeeId(entity.getEmployeeId());
        resp.setLeaveType(entity.getLeaveType());
        resp.setStartDate(entity.getStartDate());
        resp.setEndDate(entity.getEndDate());
        resp.setStatus(entity.getStatus());
        resp.setReason(entity.getReason());
        resp.setApproverId(entity.getApproverId());
        resp.setCreatedAt(entity.getCreatedAt());
        resp.setUpdatedAt(entity.getUpdatedAt());
        return resp;
    }
}
