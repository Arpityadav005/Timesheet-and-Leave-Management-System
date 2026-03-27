package com.tms.ls.messaging;

import com.tms.ls.dto.ApprovalCompletedEvent;
import com.tms.ls.service.LeaveService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class ApprovalEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(ApprovalEventConsumer.class);
    private final LeaveService leaveService;

    public ApprovalEventConsumer(LeaveService leaveService) {
        this.leaveService = leaveService;
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "leave.approval.completed", durable = "true"),
            exchange = @Exchange(value = "admin.exchange", type = "topic"),
            key = "approval.completed"
    ))
    public void handleApprovalCompleted(ApprovalCompletedEvent event) {
        if (!"LEAVE".equalsIgnoreCase(event.getTargetType())) {
            return;
        }

        log.info("Received approval event for leave {}: {}", 
                event.getTargetId(), event.getStatus());

        Map<String, String> commentsMap = new HashMap<>();
        if (event.getComments() != null) {
            commentsMap.put("comments", event.getComments());
        }

        try {
            if ("APPROVED".equalsIgnoreCase(event.getStatus())) {
                leaveService.approveLeave(event.getTargetId(), commentsMap);
            } else if ("REJECTED".equalsIgnoreCase(event.getStatus())) {
                leaveService.rejectLeave(event.getTargetId(), commentsMap);
            } else {
                log.warn("Unknown approval status: {}", event.getStatus());
            }
        } catch (Exception e) {
            log.error("Failed to process approval for leave {}", event.getTargetId(), e);
        }
    }
}
