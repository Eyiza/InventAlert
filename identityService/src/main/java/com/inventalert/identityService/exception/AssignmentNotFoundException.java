package com.inventalert.identityService.exception;

public class AssignmentNotFoundException extends IdentityServiceException {
    public AssignmentNotFoundException(String assignmentId) {
        super("Assignment not found: " + assignmentId);
    }
}
