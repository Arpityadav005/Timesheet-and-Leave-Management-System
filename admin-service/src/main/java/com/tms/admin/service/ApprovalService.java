package com.tms.admin.service;

import com.tms.admin.dto.ApprovalTaskResponse;
import java.util.List;

public interface ApprovalService {
    List<ApprovalTaskResponse> getPendingApprovals(String approverId);
    ApprovalTaskResponse approveTask(String taskId, String comments, String approverId);
    ApprovalTaskResponse rejectTask(String taskId, String comments, String approverId);
}
