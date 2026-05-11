package com.inventalert.inventoryService.exception;

import com.inventalert.inventoryService.dto.response.CsvImportErrorResponse;
import lombok.Getter;

import java.util.List;

@Getter
public class CsvImportException extends RuntimeException {
    private final int totalRows;
    private final List<CsvImportErrorResponse.RowError> errors;

    public CsvImportException(int totalRows, List<CsvImportErrorResponse.RowError> errors) {
        super("CSV import failed with " + errors.size() + " error(s)");
        this.totalRows = totalRows;
        this.errors = errors;
    }
}
