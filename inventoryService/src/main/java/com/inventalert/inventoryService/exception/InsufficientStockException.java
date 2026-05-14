package com.inventalert.inventoryService.exception;

public class InsufficientStockException extends RuntimeException {
    public InsufficientStockException(int available, int requested) {
        super("Not enough stock to complete this — only " + available + " unit(s) available, but " + requested + " were requested.");
    }
}
