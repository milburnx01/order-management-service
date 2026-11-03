package com.example.inventoryservice.mapper;

import com.example.inventoryservice.dto.CreateProductRequest;
import com.example.inventoryservice.dto.ProductResponse;
import com.example.inventoryservice.entity.Product;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    ProductResponse toProductResponse(Product product);

    Product toProduct(CreateProductRequest createProductRequest);
}
