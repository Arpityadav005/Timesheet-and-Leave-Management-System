package com.tms.ls.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "leave_policies", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"leave_type"})
})
public class LeavePolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(name = "leave_type", nullable = false, unique = true)
    private LeaveType leaveType;

    @Column(name = "days_allowed", nullable = false, precision = 5, scale = 2)
    private BigDecimal daysAllowed;

    @Column(name = "carry_forward_allowed", nullable = false)
    private boolean carryForwardAllowed = false;

    @Column(name = "max_carry_forward_days", precision = 5, scale = 2)
    private BigDecimal maxCarryForwardDays;

    @Column(name = "requires_delegate", nullable = false)
    private boolean requiresDelegate = false;

    @Column(name = "min_days_notice", nullable = false)
    private Integer minDaysNotice = 0;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public LeavePolicy() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

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

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}