package com.tms.ts.service;

import com.tms.ts.client.AuthServiceClient;
import com.tms.common.exception.ResourceNotFoundException;
import com.tms.common.exception.UnauthorizedException;
import com.tms.common.util.IdGeneratorUtil;
import com.tms.ts.dto.*;
import com.tms.ts.entity.*;
import com.tms.ts.event.TimesheetSubmittedEvent;
import com.tms.ts.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class TimesheetServiceImpl implements TimesheetService {
    private static final Logger log = LoggerFactory.getLogger(TimesheetServiceImpl.class);

    private final TimesheetRepository timesheetRepository;
    private final TimesheetEntryRepository timesheetEntryRepository;
    private final ProjectRepository projectRepository;
    private final AuthServiceClient authServiceClient;
    private final IdGeneratorUtil idGeneratorUtil;
    private final RabbitTemplate rabbitTemplate;
    private static final BigDecimal MAX_DAILY_HOURS = new BigDecimal("24.0");

    public TimesheetServiceImpl(TimesheetRepository timesheetRepository,
                                 TimesheetEntryRepository timesheetEntryRepository,
                                 ProjectRepository projectRepository,
                                 AuthServiceClient authServiceClient,
                                 IdGeneratorUtil idGeneratorUtil,
                                 RabbitTemplate rabbitTemplate) {
        this.timesheetRepository = timesheetRepository;
        this.timesheetEntryRepository = timesheetEntryRepository;
        this.projectRepository = projectRepository;
        this.authServiceClient = authServiceClient;
        this.idGeneratorUtil = idGeneratorUtil;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    @Transactional
    public TimesheetEntryResponse addEntry(TimesheetEntryRequest request, String employeeId, String authorization) {
        log.info("Adding timesheet entry for employeeId={} workDate={} projectId={}", employeeId, request.getWorkDate(), request.getProjectId());

        validateWorkDate(request.getWorkDate());

        LocalDate weekStart = request.getWorkDate().with(DayOfWeek.MONDAY);

        Project project = projectRepository.findById(request.getProjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        if (!project.isActive()) {
            log.warn("Timesheet entry rejected because projectId={} is inactive", request.getProjectId());
            throw new UnauthorizedException("Project is not active");
        }

        Timesheet timesheet = timesheetRepository
                .findByEmployeeIdAndWeekStart(employeeId, weekStart)
                .orElseGet(() -> createTimesheet(employeeId, weekStart, authorization));

        if (timesheet.getStatus() != TimesheetStatus.DRAFT
                && timesheet.getStatus() != TimesheetStatus.REJECTED) {
            log.warn("Timesheet entry rejected because timesheetId={} has status={}", timesheet.getId(), timesheet.getStatus());
            throw new UnauthorizedException("Cannot add entry to a submitted or approved timesheet");
        }

        if (timesheetEntryRepository.existsByTimesheetIdAndWorkDateAndProjectId(
                timesheet.getId(), request.getWorkDate(), request.getProjectId())) {
            log.warn("Timesheet entry rejected because duplicate exists for timesheetId={} workDate={} projectId={}", timesheet.getId(), request.getWorkDate(), request.getProjectId());
            throw new UnauthorizedException("Entry already exists for this date and project");
        }

        BigDecimal existingHours = getHoursForEmployeeDate(employeeId, request.getWorkDate());
        validateDailyHours(existingHours.add(request.getHoursWorked()),
                "Daily hours cannot exceed " + MAX_DAILY_HOURS + ". You have already logged "
                        + existingHours + " hours for " + request.getWorkDate());

        TimesheetEntry entry = new TimesheetEntry();
        entry.setId(idGeneratorUtil.generateId("TE"));
        applyEntryRequest(entry, request);
        entry.setTimesheet(timesheet);

        TimesheetEntry savedEntry = timesheetEntryRepository.save(entry);
        log.info("Timesheet entry created entryId={} timesheetId={}", savedEntry.getId(), timesheet.getId());

        return mapToEntryResponse(savedEntry, project.getName());
    }

    @Override
    @Transactional
    public TimesheetEntryResponse updateEntry(String entryId,
                                               TimesheetEntryRequest request,
                                               String employeeId) {
        log.info("Updating timesheet entry entryId={} employeeId={}", entryId, employeeId);

        TimesheetEntry entry = timesheetEntryRepository.findById(entryId)
                .orElseThrow(() -> new ResourceNotFoundException("Entry not found"));

        if (!entry.getTimesheet().getEmployeeId().equals(employeeId)) {
            log.warn("Timesheet entry update denied for entryId={} employeeId={}", entryId, employeeId);
            throw new UnauthorizedException("You can only update your own entries");
        }

        if (entry.getTimesheet().getStatus() != TimesheetStatus.DRAFT
                && entry.getTimesheet().getStatus() != TimesheetStatus.REJECTED) {
            log.warn("Timesheet entry update rejected because timesheetId={} has status={}", entry.getTimesheet().getId(), entry.getTimesheet().getStatus());
            throw new UnauthorizedException("Cannot update entry in a submitted or approved timesheet");
        }

        validateWorkDate(request.getWorkDate());

        Project project = projectRepository.findById(request.getProjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        BigDecimal existingDateHours = getHoursForEmployeeDate(employeeId, request.getWorkDate());
        BigDecimal newTotal = existingDateHours.subtract(entry.getHoursWorked()).add(request.getHoursWorked());
        validateDailyHours(newTotal,
                "Daily hours cannot exceed " + MAX_DAILY_HOURS
                        + ". Updating this entry would exceed the limit for " + request.getWorkDate());

        applyEntryRequest(entry, request);

        TimesheetEntry savedEntry = timesheetEntryRepository.save(entry);
        log.info("Timesheet entry updated entryId={} timesheetId={}", savedEntry.getId(), savedEntry.getTimesheet().getId());

        return mapToEntryResponse(savedEntry, project.getName());
    }

    @Override
    @Transactional
    public void deleteEntry(String entryId, String employeeId) {
        log.info("Deleting timesheet entry entryId={} employeeId={}", entryId, employeeId);

        TimesheetEntry entry = timesheetEntryRepository.findById(entryId)
                .orElseThrow(() -> new ResourceNotFoundException("Entry not found"));

        if (!entry.getTimesheet().getEmployeeId().equals(employeeId)) {
            log.warn("Timesheet entry delete denied for entryId={} employeeId={}", entryId, employeeId);
            throw new UnauthorizedException("You can only delete your own entries");
        }

        if (entry.getTimesheet().getStatus() != TimesheetStatus.DRAFT
                && entry.getTimesheet().getStatus() != TimesheetStatus.REJECTED) {
            log.warn("Timesheet entry delete rejected because timesheetId={} has status={}", entry.getTimesheet().getId(), entry.getTimesheet().getStatus());
            throw new UnauthorizedException("Cannot delete entry from a submitted or approved timesheet");
        }

        timesheetEntryRepository.delete(entry);
        log.info("Timesheet entry deleted entryId={}", entryId);
    }

    @Override
    public TimesheetResponse getTimesheet(LocalDate weekStart, String employeeId) {
        log.debug("Loading timesheet for employeeId={} weekStart={}", employeeId, weekStart);

        Timesheet timesheet = timesheetRepository
                .findByEmployeeIdAndWeekStart(employeeId, weekStart)
                .orElseThrow(() -> new ResourceNotFoundException("Timesheet not found for this week"));

        return mapToTimesheetResponse(timesheet);
    }

    @Override
    public List<TimesheetResponse> getAllTimesheets(String employeeId) {
        log.debug("Loading all timesheets for employeeId={}", employeeId);
        List<Timesheet> timesheets = timesheetRepository.findByEmployeeId(employeeId);
        return timesheets.stream()
                .map(this::mapToTimesheetResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public TimesheetResponse submitTimesheet(TimesheetSubmitRequest request, String employeeId, String authorization) {
        log.info("Submitting timesheet for employeeId={} weekStart={}", employeeId, request.getWeekStart());

        Timesheet timesheet = timesheetRepository
                .findByEmployeeIdAndWeekStart(employeeId, request.getWeekStart())
                .orElseThrow(() -> new ResourceNotFoundException("Timesheet not found for this week"));

        if (timesheet.getStatus() != TimesheetStatus.DRAFT
                && timesheet.getStatus() != TimesheetStatus.REJECTED) {
            log.warn("Timesheet submission rejected because timesheetId={} has status={}", timesheet.getId(), timesheet.getStatus());
            throw new UnauthorizedException("Timesheet is already submitted or approved");
        }

        if (timesheet.getEntries().isEmpty()) {
            log.warn("Timesheet submission rejected because timesheetId={} has no entries", timesheet.getId());
            throw new UnauthorizedException("Cannot submit an empty timesheet");
        }

        TimesheetValidationResponse validation = validateTimesheet(request.getWeekStart(), employeeId);
        if (!validation.isValid()) {
            log.warn("Timesheet submission rejected for timesheetId={} because validation failed: {}", timesheet.getId(), validation.getErrors());
            throw new UnauthorizedException("Cannot submit timesheet: " + String.join(", ", validation.getErrors()));
        }

        timesheet.setStatus(TimesheetStatus.SUBMITTED);
        timesheet.setComments(request.getComments());
        timesheet.setSubmittedAt(LocalDateTime.now());
        String approverId = authServiceClient.getManagerIdForEmployee(employeeId, authorization);
        if (approverId == null || approverId.isBlank()) {
            log.warn("Timesheet submission rejected for employeeId={} because no manager was assigned", employeeId);
            throw new UnauthorizedException("Cannot submit timesheet: No manager assigned to approve");
        }
        timesheet.setManagerId(approverId);

        Timesheet savedTimesheet = timesheetRepository.save(timesheet);

        // approverId comes from the manager set on the employee — passed through so
        // admin-service can create the ApprovalTask with the right approver.

        TimesheetSubmittedEvent event = new TimesheetSubmittedEvent(
                savedTimesheet.getId(),
                savedTimesheet.getEmployeeId(),
                approverId,
                savedTimesheet.getWeekStart()
        );
        rabbitTemplate.convertAndSend(
                com.tms.ts.config.RabbitMQConfig.EXCHANGE,
                com.tms.ts.config.RabbitMQConfig.TIMESHEET_ROUTING_KEY,
                event);
        log.info("Timesheet submitted timesheetId={} employeeId={} approverId={}", savedTimesheet.getId(), employeeId, approverId);

        return mapToTimesheetResponse(savedTimesheet);
    }


    @Override
    @Transactional
    public void approveTimesheet(String id, Map<String, String> comments) {
        log.info("Approving timesheet id={}", id);
        Timesheet timesheet = timesheetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Timesheet not found"));
        timesheet.setStatus(TimesheetStatus.APPROVED);
        timesheet.setApprovedAt(LocalDateTime.now());
        String comment = getComment(comments);
        if (comment != null) {
            timesheet.setComments(comment);
        }
        timesheetRepository.save(timesheet);
        log.info("Timesheet approved id={}", id);
    }

    @Override
    @Transactional
    public void rejectTimesheet(String id, Map<String, String> comments) {
        log.info("Rejecting timesheet id={}", id);
        Timesheet timesheet = timesheetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Timesheet not found"));
        timesheet.setStatus(TimesheetStatus.REJECTED);
        String comment = getComment(comments);
        if (comment != null) {
            timesheet.setRejectionReason(comment);
        }
        timesheetRepository.save(timesheet);
        log.info("Timesheet rejected id={}", id);
    }

    @Override
    public TimesheetValidationResponse validateTimesheet(LocalDate weekStart, String employeeId) {
        log.debug("Validating timesheet for employeeId={} weekStart={}", employeeId, weekStart);
        Timesheet timesheet = timesheetRepository
                .findByEmployeeIdAndWeekStart(employeeId, weekStart)
                .orElseThrow(() -> new ResourceNotFoundException("Timesheet not found for this week"));

        List<TimesheetEntry> entries = timesheet.getEntries();
        List<String> errors = new ArrayList<>();
        BigDecimal totalHours = BigDecimal.ZERO;

        for (int i = 0; i < 5; i++) { // Monday to Friday (0 to 4 days added to weekStart)
            LocalDate workday = weekStart.plusDays(i);
            boolean hasEntry = entries.stream().anyMatch(e -> e.getWorkDate().equals(workday));
            if (!hasEntry) {
                errors.add("Missing entries for " + workday.getDayOfWeek().toString() + " (" + workday + ")");
            }
        }

        for (TimesheetEntry e : entries) {
            totalHours = totalHours.add(e.getHoursWorked());
        }
        log.debug("Timesheet validation completed for timesheetId={} valid={} totalHours={}", timesheet.getId(), errors.isEmpty(), totalHours);

        return new TimesheetValidationResponse(errors.isEmpty(), errors, totalHours);
    }

    private Timesheet createTimesheet(String employeeId, LocalDate weekStart, String authorization) {
        log.debug("Creating draft timesheet for employeeId={} weekStart={}", employeeId, weekStart);
        Timesheet timesheet = new Timesheet();
        timesheet.setId(idGeneratorUtil.generateId("TS"));
        timesheet.setEmployeeId(employeeId);
        timesheet.setWeekStart(weekStart);
        timesheet.setStatus(TimesheetStatus.DRAFT);
        String approverId = authServiceClient.getManagerIdForEmployee(employeeId, authorization);
        if (approverId != null && !approverId.isBlank()) {
            timesheet.setManagerId(approverId);
            log.debug("Draft timesheet assigned managerId={} for employeeId={}", approverId, employeeId);
        }
        return timesheetRepository.save(timesheet);
    }

    private void validateWorkDate(LocalDate workDate) {
        if (workDate.isAfter(LocalDate.now())) {
            log.warn("Work date validation failed because date={} is in the future", workDate);
            throw new UnauthorizedException("Work date cannot be in the future");
        }
    }

    private BigDecimal getHoursForEmployeeDate(String employeeId, LocalDate workDate) {
        BigDecimal hours = timesheetEntryRepository.getTotalHoursForEmployeeAndDate(employeeId, workDate);
        return hours == null ? BigDecimal.ZERO : hours;
    }

    private void validateDailyHours(BigDecimal totalHours, String message) {
        if (totalHours.compareTo(MAX_DAILY_HOURS) > 0) {
            log.warn("Daily hour validation failed because totalHours={} exceeded max={}", totalHours, MAX_DAILY_HOURS);
            throw new UnauthorizedException(message);
        }
    }

    private void applyEntryRequest(TimesheetEntry entry, TimesheetEntryRequest request) {
        entry.setWorkDate(request.getWorkDate());
        entry.setProjectId(request.getProjectId());
        entry.setHoursWorked(request.getHoursWorked());
        entry.setTaskSummary(request.getTaskSummary());
        entry.setTaskId(request.getTaskId());
        entry.setActivityId(request.getActivityId());
    }

    private String getComment(Map<String, String> comments) {
        if (comments == null) {
            return null;
        }
        return comments.get("comments");
    }

    private TimesheetEntryResponse mapToEntryResponse(TimesheetEntry entry, String projectName) {
        TimesheetEntryResponse response = new TimesheetEntryResponse();
        response.setId(entry.getId());
        response.setTimesheetId(entry.getTimesheet().getId());
        response.setWorkDate(entry.getWorkDate());
        response.setProjectId(entry.getProjectId());
        response.setProjectName(projectName);
        response.setHoursWorked(entry.getHoursWorked());
        response.setTaskSummary(entry.getTaskSummary());
        response.setTaskId(entry.getTaskId());
        response.setActivityId(entry.getActivityId());
        response.setCreatedAt(entry.getCreatedAt());
        response.setUpdatedAt(entry.getUpdatedAt());
        return response;
    }

    private TimesheetResponse mapToTimesheetResponse(Timesheet timesheet) {
        TimesheetResponse response = new TimesheetResponse();
        response.setId(timesheet.getId());
        response.setEmployeeId(timesheet.getEmployeeId());
        response.setWeekStart(timesheet.getWeekStart());
        response.setStatus(timesheet.getStatus());
        response.setComments(timesheet.getComments());
        response.setSubmittedAt(timesheet.getSubmittedAt());
        response.setApprovedAt(timesheet.getApprovedAt());
        response.setApprovedBy(timesheet.getApprovedBy());
        response.setRejectionReason(timesheet.getRejectionReason());
        response.setCreatedAt(timesheet.getCreatedAt());
        response.setUpdatedAt(timesheet.getUpdatedAt());

        List<TimesheetEntryResponse> entries = timesheet.getEntries().stream()
                .map(entry -> {
                    String projectName = projectRepository.findById(entry.getProjectId())
                            .map(Project::getName)
                            .orElse("Unknown Project");
                    return mapToEntryResponse(entry, projectName);
                })
                .collect(Collectors.toList());

        response.setEntries(entries);
        return response;
    }
}
