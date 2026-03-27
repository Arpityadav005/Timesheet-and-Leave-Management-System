package com.tms.ls.dto;

import com.tms.ls.entity.LeaveType;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class LeavePolicyDto {

    @NotNull(message = "Leave type is required")
    private LeaveType leaveType;
    
    @NotNull(message = "Days allowed is required")
    private BigDecimal daysAllowed;
    
    private boolean carryForwardAllowed;
    private BigDecimal maxCarryForwardDays;
    private boolean requiresDelegate;
    private Integer minDaysNotice;
    private boolean active = true;

    public LeaveType getLeaveType() { return leaveType; }
    public void setLeaveType(LeaveType leaveType) { this.leaveType = leaveType; }

    public BigDecimal getDaysAllowed() { return daysAllowed; }
    public void setDaysAllowed(BigDecimal daysAllowed) { this.daysAllowed = daysAllowed; }

    public boolean isCarryForwardAllowed() { return carryForwardAllowed; }
    public void setCarryForwardAllowed(boolean carryForwardAllowed) { this.carryForwardAllowed = carryForwardAllowed; }

    public BigDecimal getMaxCarryForwardDays() { return maxCarryForwardDays; }
    public void setMaxCarryForwardDays(BigDecimal maxCarryForwardDays) { this.maxCarryForwardDays = maxCarryForwardDays; }

    public boolean isRequiresDelegate() { return requiresDelegate; }
    public void setRequiresDelegate(boolean requiresDelegate) { this.requiresDelegate = requiresDelegate; }

    public Integer getMinDaysNotice() { return minDaysNotice; }
    public void setMinDaysNotice(Integer minDaysNotice) { this.minDaysNotice = minDaysNotice; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
