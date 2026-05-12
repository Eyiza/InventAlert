package com.inventalert.analyticsService.exception;

public class ClickHouseQueryException extends RuntimeException {
    public ClickHouseQueryException(String message, Throwable cause) {
        super(message, cause);
    }
}
