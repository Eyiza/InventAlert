package com.inventalert.inventoryService.exception;

public class SelfApprovalException extends RuntimeException {
    public SelfApprovalException() {
        super("Self-approval is not permitted");
    }
}
