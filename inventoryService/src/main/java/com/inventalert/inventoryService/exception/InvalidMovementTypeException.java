package com.inventalert.inventoryService.exception;

public class InvalidMovementTypeException extends RuntimeException {
    public InvalidMovementTypeException(String type) {
        super("Only stock intake and sales can be recorded through this form.");
    }
}
