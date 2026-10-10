package com.example.orderservice.config;

import com.example.orderservice.grpc.InventoryServiceGrpc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.grpc.client.GrpcChannelFactory;
import org.springframework.grpc.client.ImportGrpcClients;

@Configuration
@ImportGrpcClients
public class GrpcClientConfig {

    @Bean
    public InventoryServiceGrpc.InventoryServiceBlockingStub inventoryServiceBlockingStub(GrpcChannelFactory channelFactory) {
        var channel = channelFactory.createChannel("inventory");
        return InventoryServiceGrpc.newBlockingStub(channel);
    }
}
