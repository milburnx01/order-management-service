package com.example.orderservice.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;

@OpenAPIDefinition(
        info = @Info(
                title = "Order Service",
                description = "API для создания заказов",
                version = "1.0.0",
                contact = @Contact(
                        name = "Anisimov Andrey"
                )
        )
)
public class OpenApiConfig {
}
