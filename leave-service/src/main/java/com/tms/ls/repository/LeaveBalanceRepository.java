package com.tms.ls.repository;

import com.tms.ls.entity.LeaveBalance;
import com.tms.ls.entity.LeaveType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LeaveBalanceRepository extends JpaRepository<LeaveBalance, String> {
    List<LeaveBalance> findByEmployeeId(String employeeId);
    Optional<LeaveBalance> findByEmployeeIdAndLeaveType(String employeeId, LeaveType leaveType);
}
