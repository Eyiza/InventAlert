package com.inventalert.inventoryService.repository;

import com.inventalert.inventoryService.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, String> {
    List<Product> findByIsActiveTrue();
    Optional<Product> findBySkuAndIsActiveTrue(String sku);
    Optional<Product> findByIdAndIsActiveTrue(String id);
    boolean existsBySku(String sku);
}
