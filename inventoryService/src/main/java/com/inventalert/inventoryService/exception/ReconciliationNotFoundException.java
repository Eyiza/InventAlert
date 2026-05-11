package com.inventalert.inventoryService.exception;

public class ReconciliationNotFoundException extends RuntimeException {
    public ReconciliationNotFoundException(String id) {
        super("Reconciliation not found: " + id);
    }
}
