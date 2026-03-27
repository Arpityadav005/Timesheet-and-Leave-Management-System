package com.tms.admin.repository;

import com.tms.admin.entity.ApprovalStatus;
import com.tms.admin.entity.ApprovalTask;
import com.tms.admin.entity.TargetType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApprovalTaskRepository extends JpaRepository<ApprovalTask, String> {
    List<ApprovalTask> findByApproverIdAndStatusOrderByCreatedAtDesc(String approverId, ApprovalStatus status);
    Optional<ApprovalTask> findByTargetTypeAndTargetId(TargetType targetType, String targetId);
    long countByStatus(ApprovalStatus status);
    long countByTargetType(TargetType targetType);
    long countByTargetTypeAndStatus(TargetType targetType, ApprovalStatus status);
    long countByEmployeeId(String employeeId);
    long countByEmployeeIdAndStatus(String employeeId, ApprovalStatus status);
    long countByEmployeeIdAndTargetType(String employeeId, TargetType targetType);
}
