package com.tms.ls.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "leave_balances", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"employee_id", "leave_type"})
})
public class LeaveBalance {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "employee_id", nullable = false)
    private String employeeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "leave_type", nullable = false)
    private LeaveType leaveType;

    @Column(name = "total_allowed", nullable = false, precision = 5, scale = 2)
    private BigDecimal totalAllowed;

    @Column(name = "used", nullable = false, precision = 5, scale = 2)
    private BigDecimal used;

    @Column(name = "pending", nullable = false, precision = 5, scale = 2)
    private BigDecimal pending;

    public LeaveBalance() {
        this.used = BigDecimal.ZERO;
        this.pending = BigDecimal.ZERO;
    }

    public LeaveBalance(String employeeId, LeaveType leaveType, BigDecimal totalAllowed) {
        this.employeeId = employeeId;
        this.leaveType = leaveType;
        this.totalAllowed = totalAllowed;
        this.used = BigDecimal.ZERO;
        this.pending = BigDecimal.ZERO;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public LeaveType getLeaveType() {
        return leaveType;
    }

    public void setLeaveType(LeaveType leaveType) {
        this.leaveType = leaveType;
    }

    public BigDecimal getTotalAllowed() {
        return totalAllowed;
    }

    public void setTotalAllowed(BigDecimal totalAllowed) {
        this.totalAllowed = totalAllowed;
    }

    public BigDecimal getUsed() {
        return used;
    }

    public void setUsed(BigDecimal used) {
        this.used = used;
    }

    public BigDecimal getPending() {
        return pending;
    }

    public void setPending(BigDecimal pending) {
        this.pending = pending;
    }
}
