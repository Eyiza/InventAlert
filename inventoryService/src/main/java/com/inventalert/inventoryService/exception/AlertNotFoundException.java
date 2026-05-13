package com.inventalert.inventoryService.exception;

public class AlertNotFoundException extends RuntimeException {
    public AlertNotFoundException(String id) {
        super("Restock alert not found.");
    }
}
