package com.inventalert.inventoryService.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CsvImportErrorResponse {
    private int totalRows;
    private int failedRows;
    private List<RowError> errors;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class RowError {
        private int row;
        private String message;
    }
}
