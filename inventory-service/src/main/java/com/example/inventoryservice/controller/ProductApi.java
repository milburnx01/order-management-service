package com.example.inventoryservice.controller;

import com.example.inventoryservice.dto.CreateProductRequest;
import com.example.inventoryservice.dto.ProductResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.UUID;

@Tag(name = "Товары", description = "Контроллер для управления товарами")
public interface ProductApi {

    @Operation(summary = "Найти все товары",
            description = "Позволяет получить информацию о всех товарах, находящихся на складе")
    ResponseEntity<Page<ProductResponse>> findAll(@RequestParam("page") int page, @RequestParam("size") int size);

    @Operation(summary = "Найти товар по id", description = "Позволяет найти товар на складе по его id")
    ResponseEntity<ProductResponse> findById(@PathVariable("productId") UUID id);

    @Operation(summary = "Создание товара")
    ResponseEntity<ProductResponse> createProduct(@RequestBody CreateProductRequest createProductRequest,  UriComponentsBuilder uriComponentsBuilder);

    @Operation(summary = "Удаление товара", description = "Удаление товара по его id")
    ResponseEntity<Void> deleteProduct(@PathVariable("productId") UUID id);
}
