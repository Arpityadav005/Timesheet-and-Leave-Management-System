package com.tms.ls.repository;

import com.tms.ls.entity.LeavePolicy;
import com.tms.ls.entity.LeaveType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LeavePolicyRepository extends JpaRepository<LeavePolicy, String> {
    Optional<LeavePolicy> findByLeaveType(LeaveType leaveType);
    List<LeavePolicy> findByActiveTrue();
}
