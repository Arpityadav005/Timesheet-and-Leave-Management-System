package com.tms.ts.repository;

import com.tms.ts.entity.TimesheetEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TimesheetEntryRepository extends JpaRepository<TimesheetEntry, String> {

    List<TimesheetEntry> findByTimesheetId(String timesheetId);

    Optional<TimesheetEntry> findByTimesheetIdAndWorkDateAndProjectId(
            String timesheetId, LocalDate workDate, String projectId);

    boolean existsByTimesheetIdAndWorkDateAndProjectId(
            String timesheetId, LocalDate workDate, String projectId);

    void deleteByTimesheetId(String timesheetId);

    @Query("SELECT COALESCE(SUM(e.hoursWorked), 0) FROM TimesheetEntry e WHERE e.timesheet.employeeId = :employeeId AND e.workDate = :workDate")
    BigDecimal getTotalHoursForEmployeeAndDate(@Param("employeeId") String employeeId, @Param("workDate") LocalDate workDate);
}