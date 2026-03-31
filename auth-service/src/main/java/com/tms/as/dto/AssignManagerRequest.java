package com.tms.as.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public class AssignManagerRequest {

    @NotBlank(message = "Manager ID is required")
    @Schema(example = "USR-AB12CD34")
    private String managerId;

    public String getManagerId() {
        return managerId;
    }

    public void setManagerId(String managerId) {
        this.managerId = managerId;
    }
}
