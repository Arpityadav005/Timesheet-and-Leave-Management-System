package com.tms.ls.repository;

import com.tms.ls.entity.LeaveRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, String> {
    List<LeaveRequest> findByEmployeeIdOrderByStartDateDesc(String employeeId);
    List<LeaveRequest> findByApproverId(String approverId);

    @Query("SELECT lr FROM LeaveRequest lr WHERE lr.employeeId = :employeeId " +
           "AND lr.status != 'REJECTED' AND lr.status != 'CANCELLED' " +
           "AND ((lr.startDate <= :endDate AND lr.endDate >= :startDate))")
    List<LeaveRequest> findOverlappingRequests(String employeeId, LocalDate startDate, LocalDate endDate);
}
