package com.example.notificationservice.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;

@OpenAPIDefinition(
        info = @Info(
                title = "Notification Service Order Api",
                description = "Read-only API для аналитики", version = "1.0.0",
                contact = @Contact(
                        name = "Anisimov Andrey"
                )
        )
)
public class OpenApiConfig {
}
