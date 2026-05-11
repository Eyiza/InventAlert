package com.inventalert.inventoryService.exception.handler;

import com.inventalert.inventoryService.dto.response.CsvImportErrorResponse;
import com.inventalert.inventoryService.dto.response.ErrorResponse;
import com.inventalert.inventoryService.exception.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({
            WarehouseNotFoundException.class,
            ProductNotFoundException.class,
            StockLevelNotFoundException.class,
            TransferNotFoundException.class,
            AlertNotFoundException.class,
            ReconciliationNotFoundException.class
    })
    public ResponseEntity<ErrorResponse> handleNotFound(RuntimeException ex) {
        return error(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler({
            DuplicateSkuException.class,
            InvalidStateTransitionException.class
    })
    public ResponseEntity<ErrorResponse> handleConflict(RuntimeException ex) {
        return error(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(StockConflictException.class)
    public ResponseEntity<ErrorResponse> handleStockConflict(StockConflictException ex) {
        return error(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler({
            InsufficientStockException.class,
            WarehouseNotAssignedException.class,
            InvalidMovementTypeException.class
    })
    public ResponseEntity<ErrorResponse> handleBadRequest(RuntimeException ex) {
        return error(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(SelfApprovalException.class)
    public ResponseEntity<ErrorResponse> handleSelfApproval(SelfApprovalException ex) {
        return error(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(CsvImportException.class)
    public ResponseEntity<CsvImportErrorResponse> handleCsvImport(CsvImportException ex) {
        CsvImportErrorResponse body = CsvImportErrorResponse.builder()
                .totalRows(ex.getTotalRows())
                .failedRows(ex.getErrors().size())
                .errors(ex.getErrors())
                .build();
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            errors.put(fe.getField(), fe.getDefaultMessage());
        }
        return ResponseEntity.badRequest().body(errors);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex) {
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
    }

    private ResponseEntity<ErrorResponse> error(HttpStatus status, String message) {
        ErrorResponse body = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .build();
        return ResponseEntity.status(status).body(body);
    }
}
