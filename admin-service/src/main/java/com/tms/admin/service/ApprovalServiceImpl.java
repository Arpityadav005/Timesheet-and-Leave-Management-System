package com.tms.admin.service;

import com.tms.admin.dto.ApprovalCompletedEvent;
import com.tms.admin.dto.ApprovalTaskResponse;
import com.tms.admin.entity.ApprovalStatus;
import com.tms.admin.entity.ApprovalTask;
import com.tms.admin.repository.ApprovalTaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ApprovalServiceImpl implements ApprovalService {
    private static final Logger log = LoggerFactory.getLogger(ApprovalServiceImpl.class);

    private final ApprovalTaskRepository taskRepository;
    private final RabbitTemplate rabbitTemplate;

    public ApprovalServiceImpl(ApprovalTaskRepository taskRepository, RabbitTemplate rabbitTemplate) {
        this.taskRepository = taskRepository;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApprovalTaskResponse> getPendingApprovals(String approverId) {
        log.debug("Loading pending approvals for approverId={}", approverId);
        return taskRepository.findByApproverIdAndStatusOrderByCreatedAtDesc(approverId, ApprovalStatus.PENDING)
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ApprovalTaskResponse approveTask(String taskId, String comments, String approverId) {
        log.info("Approving task taskId={} approverId={}", taskId, approverId);
        return processTask(taskId, comments, approverId, ApprovalStatus.APPROVED);
    }

    @Override
    @Transactional
    public ApprovalTaskResponse rejectTask(String taskId, String comments, String approverId) {
        log.info("Rejecting task taskId={} approverId={}", taskId, approverId);
        return processTask(taskId, comments, approverId, ApprovalStatus.REJECTED);
    }

    private ApprovalTaskResponse processTask(String taskId, String comments, String approverId, ApprovalStatus newStatus) {
        ApprovalTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Approval Task not found with id: " + taskId));

        if (!task.getApproverId().equals(approverId)) {
            log.warn("Approval task access denied for taskId={} approverId={} expectedApproverId={}", taskId, approverId, task.getApproverId());
            throw new IllegalArgumentException("You are not authorized to approve this task");
        }

        if (task.getStatus() != ApprovalStatus.PENDING) {
            log.warn("Approval task already processed taskId={} status={}", taskId, task.getStatus());
            throw new IllegalArgumentException("Task is already processed. Current status: " + task.getStatus());
        }

        task.setStatus(newStatus);
        task.setComments(comments);

        task = taskRepository.save(task);

        ApprovalCompletedEvent event = new ApprovalCompletedEvent(
                task.getTargetType().name(),
                task.getTargetId(),
                task.getEmployeeId(),
                newStatus.name(),
                comments
        );
        
        // Publish to admin exchange so timesheet/leave services can act on it
        rabbitTemplate.convertAndSend("admin.exchange", "approval.completed", event);
        log.info("Approval task processed taskId={} newStatus={} targetType={} targetId={}", taskId, newStatus, task.getTargetType(), task.getTargetId());

        return mapToResponse(task);
    }

    private ApprovalTaskResponse mapToResponse(ApprovalTask task) {
        ApprovalTaskResponse resp = new ApprovalTaskResponse();
        resp.setId(task.getId());
        resp.setTargetType(task.getTargetType());
        resp.setTargetId(task.getTargetId());
        resp.setEmployeeId(task.getEmployeeId());
        resp.setApproverId(task.getApproverId());
        resp.setStatus(task.getStatus());
        resp.setComments(task.getComments());
        resp.setCreatedAt(task.getCreatedAt());
        resp.setUpdatedAt(task.getUpdatedAt());
        return resp;
    }
}
