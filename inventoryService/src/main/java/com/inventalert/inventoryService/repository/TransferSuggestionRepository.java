package com.inventalert.inventoryService.repository;

import com.inventalert.inventoryService.model.TransferStatus;
import com.inventalert.inventoryService.model.TransferSuggestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TransferSuggestionRepository extends JpaRepository<TransferSuggestion, String> {
    List<TransferSuggestion> findByStatus(TransferStatus status);
    Optional<TransferSuggestion> findByIdAndStatus(String id, TransferStatus status);
    List<TransferSuggestion> findByFromWarehouseIdOrToWarehouseId(String fromWarehouseId, String toWarehouseId);
}
