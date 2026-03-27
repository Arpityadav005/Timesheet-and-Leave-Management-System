package com.tms.ts.dto;

import java.math.BigDecimal;
import java.util.List;

public class TimesheetValidationResponse {

    private boolean valid;
    private List<String> errors;
    private BigDecimal totalWeeklyHours;

    public TimesheetValidationResponse() {
    }

    public TimesheetValidationResponse(boolean valid, List<String> errors, BigDecimal totalWeeklyHours) {
        this.valid = valid;
        this.errors = errors;
        this.totalWeeklyHours = totalWeeklyHours;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }

    public BigDecimal getTotalWeeklyHours() {
        return totalWeeklyHours;
    }

    public void setTotalWeeklyHours(BigDecimal totalWeeklyHours) {
        this.totalWeeklyHours = totalWeeklyHours;
    }
}
