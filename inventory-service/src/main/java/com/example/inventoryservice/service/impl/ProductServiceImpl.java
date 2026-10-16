package com.example.inventoryservice.service.impl;

import com.example.inventoryservice.dto.CreateProductRequest;
import com.example.inventoryservice.dto.ProductResponse;
import com.example.inventoryservice.entity.Product;
import com.example.inventoryservice.exception.ProductNotFoundException;
import com.example.inventoryservice.grpc.ProductAvailability;
import com.example.inventoryservice.grpc.ProductItem;
import com.example.inventoryservice.mapper.ProductMapper;
import com.example.inventoryservice.repository.ProductRepository;
import com.example.inventoryservice.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    @Override
    public Page<ProductResponse> findAll(Pageable pageable) {
        log.info("Finding all products, pageable={}", pageable);
        Page<ProductResponse> response = productRepository.findAll(pageable).map(productMapper::toProductResponse);
        log.info("Products found successfully, totalElements={}", response.getTotalElements());
        return response;
    }

    @Override
    public ProductResponse findById(UUID id) {
        log.info("Finding product by id={}", id);
        ProductResponse response = productRepository.findById(id)
                .map(productMapper::toProductResponse)
                .orElseThrow(() -> new ProductNotFoundException("Product with id " + id + " not found"));
        log.info("Product found successfully, id={}", id);
        return response;
    }

    @Override
    public ProductResponse createProduct(CreateProductRequest createProductRequest) {
        log.info("Creating product, request={}", createProductRequest);
        Product product = productMapper.toProduct(createProductRequest);
        ProductResponse response = productMapper.toProductResponse(
                productRepository.save(product));
        log.info("Product created successfully, response={}", response);
        return response;
    }

    @Override
    public void deleteProduct(UUID id) {
        log.info("Deleting product, id={}", id);
        productRepository.deleteById(id);
        log.info("Product deleted successfully, id={}", id);
    }

    @Override
    public List<ProductAvailability> checkAvailability(List<ProductItem> productItems) {
        log.info("Checking product availability, items={}", productItems);
        List<ProductAvailability> productAvailabilities = new ArrayList<>();
        for (ProductItem productItem : productItems) {
            Optional<Product> optionalProduct = productRepository.findById(UUID.fromString(productItem.getId()));
            if (optionalProduct.isEmpty()) {
                productAvailabilities.add(ProductAvailability.newBuilder()
                                .setId(productItem.getId())
                        .build());
            } else {
                Product product = optionalProduct.get();
                boolean isAvailable = product.getQuantity() >= productItem.getQuantity();
                productAvailabilities.add(ProductAvailability.newBuilder()
                                .setId(product.getId().toString())
                                .setName(product.getName())
                                .setPrice(product.getPrice().doubleValue())
                                .setSale(product.getSale().doubleValue())
                                .setAvailableQuantity(product.getQuantity())
                                .setIsAvailable(isAvailable)
                        .build());
            }
        }
        log.info("Product availability checked successfully, result={}",
                productAvailabilities);
        return productAvailabilities;
    }

    @Override
    @Transactional
    public void writeOffProducts(List<ProductItem> productItems) {
        log.info("Writing off products, items={}", productItems);
        for (ProductItem productItem : productItems) {
            Product product = productRepository.findById(UUID.fromString(productItem.getId())).orElseThrow(() ->
                    new ProductNotFoundException("Product with id " + productItem.getId() + " not found"));
            product.setQuantity(product.getQuantity() - productItem.getQuantity());
        }
        log.info("Products write-off completed, items={}", productItems);
    }
}
