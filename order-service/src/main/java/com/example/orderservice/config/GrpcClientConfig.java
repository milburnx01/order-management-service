package com.example.orderservice.config;

import com.example.orderservice.grpc.InventoryServiceGrpc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.grpc.client.GrpcChannelFactory;

@Configuration
public class GrpcClientConfig {

    @Bean
    public InventoryServiceGrpc.InventoryServiceBlockingStub inventoryStub(GrpcChannelFactory channelFactory) {
        return InventoryServiceGrpc.newBlockingStub(
                channelFactory.createChannel("inventory")
        );
    }
}
