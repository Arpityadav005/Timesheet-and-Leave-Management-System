package com.tms.ts.repository;

import com.tms.ts.entity.Timesheet;
import com.tms.ts.entity.TimesheetStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TimesheetRepository extends JpaRepository<Timesheet, String> {

    Optional<Timesheet> findByEmployeeIdAndWeekStart(String employeeId, LocalDate weekStart);

    List<Timesheet> findByEmployeeId(String employeeId);

    List<Timesheet> findByEmployeeIdAndStatus(String employeeId, TimesheetStatus status);

    List<Timesheet> findByStatus(TimesheetStatus status);

    boolean existsByEmployeeIdAndWeekStart(String employeeId, LocalDate weekStart);
}