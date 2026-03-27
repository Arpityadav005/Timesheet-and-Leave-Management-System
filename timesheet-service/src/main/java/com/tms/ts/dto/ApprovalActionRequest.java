package com.tms.ts.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Approve or reject a timesheet")
public class ApprovalActionRequest {
    @Schema(description = "Optional reviewer comments", example = "Approved after checking weekly entries")
    private String comments;

    public ApprovalActionRequest() {}

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }
}
