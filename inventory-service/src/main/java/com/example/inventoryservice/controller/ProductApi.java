package com.example.inventoryservice.controller;

import com.example.inventoryservice.dto.CreateProductRequest;
import com.example.inventoryservice.dto.ProductResponse;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.UUID;

public interface ProductApi {

    ResponseEntity<Page<ProductResponse>> findAll(@RequestParam("page") int page, @RequestParam("size") int size);

    ResponseEntity<ProductResponse> findById(@PathVariable("productId") UUID id);

    ResponseEntity<ProductResponse> createProduct(@RequestBody CreateProductRequest createProductRequest,  UriComponentsBuilder uriComponentsBuilder);

    ResponseEntity<Void> deleteProduct(@PathVariable("productId") UUID id);


}
