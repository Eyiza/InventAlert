package com.inventalert.inventoryService.exception;

public class CsvParseException extends RuntimeException {

    public CsvParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
