package com.lankatech.ems.exception;

// Thrown when a role-based rule is broken (e.g. a Payroll Executive
// tries to approve leave, or a supervisor tries to view another
// department's attendance). HTTP 403.
public class UnauthorizedActionException extends RuntimeException {
    public UnauthorizedActionException(String message) {
        super(message);
    }
}
