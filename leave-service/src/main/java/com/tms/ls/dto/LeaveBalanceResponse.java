package com.tms.ls.dto;

import com.tms.ls.entity.LeaveType;
import java.math.BigDecimal;

public class LeaveBalanceResponse {
    private String id;
    private String employeeId;
    private LeaveType leaveType;
    private BigDecimal totalAllowed;
    private BigDecimal used;
    private BigDecimal pending;

    public LeaveBalanceResponse() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }

    public LeaveType getLeaveType() { return leaveType; }
    public void setLeaveType(LeaveType leaveType) { this.leaveType = leaveType; }

    public BigDecimal getTotalAllowed() { return totalAllowed; }
    public void setTotalAllowed(BigDecimal totalAllowed) { this.totalAllowed = totalAllowed; }

    public BigDecimal getUsed() { return used; }
    public void setUsed(BigDecimal used) { this.used = used; }

    public BigDecimal getPending() { return pending; }
    public void setPending(BigDecimal pending) { this.pending = pending; }
}
