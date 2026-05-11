ALTER TABLE WarehouseAssignment ADD INDEX idx_warehouseAssignment_userId (userId);
ALTER TABLE User ADD INDEX idx_user_companyId (companyId);
ALTER TABLE password_reset_tokens ADD INDEX idx_prt_userId (userId);
