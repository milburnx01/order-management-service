package com.example.inventoryservice.service;

import com.example.inventoryservice.dto.CreateProductRequest;
import com.example.inventoryservice.dto.ProductResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ProductService {

    Page<ProductResponse> findAll(Pageable pageable);

    ProductResponse findById(UUID id);

    ProductResponse createProduct(CreateProductRequest createProductRequest);

    void deleteProduct(UUID id);
}
