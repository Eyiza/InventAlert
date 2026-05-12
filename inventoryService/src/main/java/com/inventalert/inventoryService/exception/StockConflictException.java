package com.inventalert.inventoryService.exception;

public class StockConflictException extends RuntimeException {

    public StockConflictException() {
        super("Stock was updated by another user while processing your request. " +
              "Please refresh your stock view and try again.");
    }
}
