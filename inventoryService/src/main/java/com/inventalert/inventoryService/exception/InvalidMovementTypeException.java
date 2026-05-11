package com.inventalert.inventoryService.exception;

public class InvalidMovementTypeException extends RuntimeException {
    public InvalidMovementTypeException(String type) {
        super("Invalid movement type via this endpoint: " + type + ". Use INTAKE or OUTBOUND_SALE only.");
    }
}
