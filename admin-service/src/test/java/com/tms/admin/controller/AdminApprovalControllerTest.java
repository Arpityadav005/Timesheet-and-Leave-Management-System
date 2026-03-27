package com.tms.admin.controller;

import com.tms.admin.dto.ApprovalActionRequest;
import com.tms.admin.dto.ApprovalTaskResponse;
import com.tms.admin.entity.ApprovalStatus;
import com.tms.admin.service.ApprovalService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminApprovalControllerTest {

    @Mock
    private ApprovalService approvalService;

    @InjectMocks
    private AdminApprovalController controller;

    @Test
    void getPendingApprovals_ReturnsOkResponse() {
        ApprovalTaskResponse task = new ApprovalTaskResponse();
        task.setId("TASK-1");
        when(approvalService.getPendingApprovals("EMP-1")).thenReturn(List.of(task));

        ResponseEntity<List<ApprovalTaskResponse>> response = controller.getPendingApprovals("EMP-1");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void approveTask_WithBody_ReturnsOkResponse() {
        ApprovalActionRequest request = new ApprovalActionRequest();
        request.setComments("Approved");

        ApprovalTaskResponse task = new ApprovalTaskResponse();
        task.setStatus(ApprovalStatus.APPROVED);
        when(approvalService.approveTask("TASK-1", "Approved", "EMP-1")).thenReturn(task);

        ResponseEntity<ApprovalTaskResponse> response = controller.approveTask("TASK-1", request, "EMP-1");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(ApprovalStatus.APPROVED, response.getBody().getStatus());
    }

    @Test
    void approveTask_WithoutBody_DelegatesNullComments() {
        ApprovalTaskResponse task = new ApprovalTaskResponse();
        when(approvalService.approveTask("TASK-1", null, "EMP-1")).thenReturn(task);

        ResponseEntity<ApprovalTaskResponse> response = controller.approveTask("TASK-1", null, "EMP-1");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(approvalService).approveTask("TASK-1", null, "EMP-1");
    }

    @Test
    void rejectTask_ReturnsOkResponse() {
        ApprovalActionRequest request = new ApprovalActionRequest();
        request.setComments("Rejected");

        ApprovalTaskResponse task = new ApprovalTaskResponse();
        task.setStatus(ApprovalStatus.REJECTED);
        when(approvalService.rejectTask("TASK-1", "Rejected", "EMP-1")).thenReturn(task);

        ResponseEntity<ApprovalTaskResponse> response = controller.rejectTask("TASK-1", request, "EMP-1");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(ApprovalStatus.REJECTED, response.getBody().getStatus());
    }
}
