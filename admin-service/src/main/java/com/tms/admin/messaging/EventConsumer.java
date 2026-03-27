package com.tms.admin.messaging;

import com.tms.admin.dto.LeaveRequestedEvent;
import com.tms.admin.dto.TimesheetSubmittedEvent;
import com.tms.admin.entity.ApprovalTask;
import com.tms.admin.entity.TargetType;
import com.tms.admin.repository.ApprovalTaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
public class EventConsumer {

    private static final Logger log = LoggerFactory.getLogger(EventConsumer.class);
    private final ApprovalTaskRepository taskRepository;

    public EventConsumer(ApprovalTaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @RabbitListener(queues = "admin.timesheet.queue")
    @Transactional
    public void handleTimesheetSubmitted(TimesheetSubmittedEvent event) {
        log.info("Received TimesheetSubmittedEvent for timesheet: {}", event.getTimesheetId());
        
        Optional<ApprovalTask> existing = taskRepository.findByTargetTypeAndTargetId(TargetType.TIMESHEET, event.getTimesheetId());
        if (existing.isEmpty()) {
            ApprovalTask task = new ApprovalTask(
                    TargetType.TIMESHEET,
                    event.getTimesheetId(),
                    event.getEmployeeId(),
                    event.getApproverId()
            );
            taskRepository.save(task);
            log.info("Approval task created for timesheet: {}", event.getTimesheetId());
        }
    }

    @RabbitListener(queues = "admin.leave.queue")
    @Transactional
    public void handleLeaveRequested(LeaveRequestedEvent event) {
        log.info("Received LeaveRequestedEvent for request: {}", event.getRequestId());

        Optional<ApprovalTask> existing = taskRepository.findByTargetTypeAndTargetId(TargetType.LEAVE, event.getRequestId());
        if (existing.isEmpty()) {
            ApprovalTask task = new ApprovalTask(
                    TargetType.LEAVE,
                    event.getRequestId(),
                    event.getEmployeeId(),
                    event.getApproverId()
            );
            taskRepository.save(task);
            log.info("Approval task created for leave request: {}", event.getRequestId());
        }
    }
}
