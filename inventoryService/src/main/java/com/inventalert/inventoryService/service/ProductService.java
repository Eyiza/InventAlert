package com.inventalert.inventoryService.service;

import com.inventalert.inventoryService.dto.request.CreateProductRequest;
import com.inventalert.inventoryService.dto.request.UpdateProductRequest;
import com.inventalert.inventoryService.dto.response.ProductResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ProductService {
    ProductResponse create(CreateProductRequest request, String createdBy);
    List<ProductResponse> list();
    ProductResponse update(String id, UpdateProductRequest request);
    void setDefaultThreshold(String id, int threshold);
    List<ProductResponse> importFromCsv(MultipartFile file, String createdBy);
}
