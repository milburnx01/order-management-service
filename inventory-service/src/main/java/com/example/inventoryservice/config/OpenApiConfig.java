package com.example.inventoryservice.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;

@OpenAPIDefinition(
        info = @Info(
                title = "Inventory Service Api",
                description = "API для товаров на складе",
                version = "1.0.0",
                contact = @Contact(
                        name = "Anisimov Andrey"
                )
        )
)
public class OpenApiConfig {
}
