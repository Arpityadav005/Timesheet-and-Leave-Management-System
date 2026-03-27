package com.tms.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Approve or reject an approval task")
public class ApprovalActionRequest {
    @Schema(description = "Optional reviewer comments", example = "Approved after checking leave balance")
    private String comments;

    public ApprovalActionRequest() {}

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }
}
