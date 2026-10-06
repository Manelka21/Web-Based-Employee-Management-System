package com.lankatech.ems.dto.request;

import jakarta.validation.constraints.Size;

// Optional body for approve/reject: { "comment": "Enjoy your holiday" }
// The comment is included in the notification sent to the employee.
public class LeaveDecisionRequest {

    @Size(max = 300)
    private String comment;

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
}
