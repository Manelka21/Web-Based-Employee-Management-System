package com.lankatech.ems.exception;

// Thrown when someone tries to move a leave request, application,
// or payroll record into a status it isn't allowed to reach from
// its current status. HTTP 400.
public class InvalidStatusTransitionException extends RuntimeException {
    public InvalidStatusTransitionException(String message) {
        super(message);
    }
}
