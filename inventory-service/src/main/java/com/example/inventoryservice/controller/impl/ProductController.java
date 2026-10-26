package com.example.inventoryservice.controller.impl;

import com.example.inventoryservice.controller.ProductApi;
import com.example.inventoryservice.dto.CreateProductRequest;
import com.example.inventoryservice.dto.ProductResponse;
import com.example.inventoryservice.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController implements ProductApi {

    private final ProductService productService;

    @GetMapping
    @Override
    public ResponseEntity<Page<ProductResponse>> findAll(@RequestParam("page") int page,
                                                         @RequestParam("size") int size) {
        return ResponseEntity.ok(productService.findAll(PageRequest.of(page, size)));
    }

    @GetMapping("/{productId}")
    @Override
    public ResponseEntity<ProductResponse> findById(@PathVariable("productId") UUID id) {
        return ResponseEntity.ok(productService.findById(id));
    }

    @PostMapping
    @Override
    public ResponseEntity<ProductResponse> createProduct(@RequestBody CreateProductRequest createProductRequest,
                                                         UriComponentsBuilder uriComponentsBuilder) {
        ProductResponse response = productService.createProduct(createProductRequest);
        URI uri = uriComponentsBuilder
                .path("/api/products/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(uri).body(response);
    }

    @DeleteMapping("/{productId}")
    @Override
    public ResponseEntity<Void> deleteProduct(@PathVariable("productId") UUID id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }
}
