package com.inventalert.inventoryService.exception;

public class TransferNotFoundException extends RuntimeException {
    public TransferNotFoundException(String id) {
        super("Transfer record not found.");
    }
}
