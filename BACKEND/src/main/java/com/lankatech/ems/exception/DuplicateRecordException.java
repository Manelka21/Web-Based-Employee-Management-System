package com.lankatech.ems.exception;

// Thrown when a unique constraint would be violated (e.g. duplicate NIC,
// duplicate email, same employee enrolled in the same program twice).
// HTTP 409.
public class DuplicateRecordException extends RuntimeException {
    public DuplicateRecordException(String message) {
        super(message);
    }
}
