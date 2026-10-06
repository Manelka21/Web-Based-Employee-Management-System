package com.lankatech.ems.exception;

// Thrown when payroll generation is attempted but attendance/leave
// data for the pay period hasn't been finalized yet.
// HTTP 409 (conflict — business rule prevents the operation).
public class PayrollNotReadyException extends RuntimeException {
    public PayrollNotReadyException(String message) {
        super(message);
    }
}
