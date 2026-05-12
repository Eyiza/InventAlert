ALTER TABLE stockLevels ADD INDEX idx_stockLevels_warehouseId (warehouseId);
ALTER TABLE transferSuggestions ADD INDEX idx_transferSuggestions_fromWarehouseId (fromWarehouseId);
ALTER TABLE transferSuggestions ADD INDEX idx_transferSuggestions_toWarehouseId (toWarehouseId);
ALTER TABLE transferSuggestions ADD INDEX idx_transferSuggestions_status (status);
