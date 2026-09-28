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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    @Override
    public Page<ProductResponse> findAll(Pageable pageable) {
        return productRepository.findAll(pageable).map(productMapper::toProductResponse);
    }

    @Override
    public ProductResponse findById(UUID id) {
        return productRepository.findById(id)
                .map(productMapper::toProductResponse)
                .orElseThrow(() -> new ProductNotFoundException("Product with id " + id + " not found"));
    }

    @Override
    public ProductResponse createProduct(CreateProductRequest createProductRequest) {
        Product product = productMapper.toProduct(createProductRequest);
        return productMapper.toProductResponse(productRepository.save(product));
    }

    @Override
    public void deleteProduct(UUID id) {
        productRepository.deleteById(id);
    }

    @Override
    public List<ProductAvailability> checkAvailability(List<ProductItem> productItems) {
        List<ProductAvailability> productAvailabilities = new ArrayList<>();
        for (ProductItem productItem : productItems) {
            Optional<Product> optionalProduct = productRepository.findById(UUID.fromString(productItem.getId()));
            if (optionalProduct.isEmpty()) {
                productAvailabilities.add(ProductAvailability.newBuilder()
                                .setId(productItem.getId())
                        .build());
            } else {
                Product product = optionalProduct.get();
                productAvailabilities.add(ProductAvailability.newBuilder()
                                .setId(product.getId().toString())
                                .setName(product.getName())
                                .setPrice(product.getPrice().doubleValue())
                                .setSale(product.getSale().doubleValue())
                                .setAvailableQuantity(product.getQuantity())
                        .build());
            }
        }
        return productAvailabilities;
    }
}
