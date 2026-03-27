package com.tms.as.dto;

import jakarta.validation.constraints.NotBlank;

public class AssignManagerRequest {

    @NotBlank(message = "Manager ID is required")
    private String managerId;

    public String getManagerId() {
        return managerId;
    }

    public void setManagerId(String managerId) {
        this.managerId = managerId;
    }
}
