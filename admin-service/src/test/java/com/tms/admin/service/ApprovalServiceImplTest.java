package com.tms.admin.service;

import com.tms.admin.dto.ApprovalCompletedEvent;
import com.tms.admin.dto.ApprovalTaskResponse;
import com.tms.admin.entity.ApprovalStatus;
import com.tms.admin.entity.ApprovalTask;
import com.tms.admin.entity.TargetType;
import com.tms.admin.repository.ApprovalTaskRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApprovalServiceImplTest {

    @Mock
    private ApprovalTaskRepository taskRepository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private ApprovalServiceImpl service;

    @Test
    void getPendingApprovals_ReturnsMappedTasks() {
        ApprovalTask task = pendingTask();
        task.setCreatedAt(LocalDateTime.of(2026, 3, 26, 10, 0));

        when(taskRepository.findByApproverIdAndStatusOrderByCreatedAtDesc("APR-1", ApprovalStatus.PENDING))
                .thenReturn(List.of(task));

        List<ApprovalTaskResponse> responses = service.getPendingApprovals("APR-1");

        assertEquals(1, responses.size());
        assertEquals("TASK-1", responses.get(0).getId());
        assertEquals(TargetType.LEAVE, responses.get(0).getTargetType());
    }

    @Nested
    class ProcessTaskTests {

        @Test
        void approveTask_UpdatesTaskAndPublishesEvent() {
            ApprovalTask task = pendingTask();

            when(taskRepository.findById("TASK-1")).thenReturn(Optional.of(task));
            when(taskRepository.save(any(ApprovalTask.class))).thenAnswer(invocation -> invocation.getArgument(0));

            ApprovalTaskResponse response = service.approveTask("TASK-1", "Looks good", "APR-1");

            assertEquals(ApprovalStatus.APPROVED, response.getStatus());
            assertEquals("Looks good", response.getComments());

            ArgumentCaptor<ApprovalCompletedEvent> eventCaptor = ArgumentCaptor.forClass(ApprovalCompletedEvent.class);
            verify(rabbitTemplate).convertAndSend(
                    eq("admin.exchange"),
                    eq("approval.completed"),
                    eventCaptor.capture());
            assertEquals("LEAVE", eventCaptor.getValue().getTargetType());
            assertEquals("TARGET-1", eventCaptor.getValue().getTargetId());
            assertEquals("EMP-1", eventCaptor.getValue().getEmployeeId());
            assertEquals("APPROVED", eventCaptor.getValue().getStatus());
            assertEquals("Looks good", eventCaptor.getValue().getComments());
        }

        @Test
        void rejectTask_UpdatesTaskAndPublishesEvent() {
            ApprovalTask task = pendingTask();

            when(taskRepository.findById("TASK-1")).thenReturn(Optional.of(task));
            when(taskRepository.save(any(ApprovalTask.class))).thenAnswer(invocation -> invocation.getArgument(0));

            ApprovalTaskResponse response = service.rejectTask("TASK-1", "Not valid", "APR-1");

            assertEquals(ApprovalStatus.REJECTED, response.getStatus());
            assertEquals("Not valid", response.getComments());
            verify(rabbitTemplate).convertAndSend(
                    eq("admin.exchange"),
                    eq("approval.completed"),
                    any(ApprovalCompletedEvent.class));
        }

        @Test
        void approveTask_ThrowsWhenTaskMissing() {
            when(taskRepository.findById("missing")).thenReturn(Optional.empty());

            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> service.approveTask("missing", "comment", "APR-1"));

            assertEquals("Approval Task not found with id: missing", exception.getMessage());
            verifyNoInteractions(rabbitTemplate);
        }

        @Test
        void approveTask_ThrowsWhenApproverMismatch() {
            ApprovalTask task = pendingTask();
            task.setApproverId("APR-2");
            when(taskRepository.findById("TASK-1")).thenReturn(Optional.of(task));

            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> service.approveTask("TASK-1", "comment", "APR-1"));

            assertEquals("You are not authorized to approve this task", exception.getMessage());
            verifyNoInteractions(rabbitTemplate);
        }

        @Test
        void approveTask_ThrowsWhenAlreadyProcessed() {
            ApprovalTask task = pendingTask();
            task.setStatus(ApprovalStatus.APPROVED);
            when(taskRepository.findById("TASK-1")).thenReturn(Optional.of(task));

            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> service.approveTask("TASK-1", "comment", "APR-1"));

            assertEquals("Task is already processed. Current status: APPROVED", exception.getMessage());
            verifyNoInteractions(rabbitTemplate);
        }
    }

    private ApprovalTask pendingTask() {
        ApprovalTask task = new ApprovalTask();
        task.setId("TASK-1");
        task.setTargetType(TargetType.LEAVE);
        task.setTargetId("TARGET-1");
        task.setEmployeeId("EMP-1");
        task.setApproverId("APR-1");
        task.setStatus(ApprovalStatus.PENDING);
        return task;
    }
}
