package com.example.orderservice.config;

import com.example.orderservice.grpc.InventoryServiceGrpc;
import org.springframework.context.annotation.Configuration;
import org.springframework.grpc.client.ImportGrpcClients;

@Configuration
@ImportGrpcClients(
        target = "inventory",
        types = InventoryServiceGrpc.InventoryServiceBlockingStub.class
)
public class GrpcClientConfig {
}
