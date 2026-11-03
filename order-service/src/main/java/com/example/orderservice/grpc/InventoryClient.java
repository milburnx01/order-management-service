package com.example.orderservice.grpc;

import com.example.orderservice.dto.CreateOrderRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class InventoryClient {

    private final InventoryServiceGrpc.InventoryServiceBlockingStub stub;

    public List<ProductAvailability> checkAvailability(CreateOrderRequest createOrderRequest) {
        List<ProductItem> items = createOrderRequest.items().stream()
                .map(productItem -> ProductItem.newBuilder()
                        .setId(productItem.id())
                        .setQuantity(productItem.quantity())
                        .build())
                .toList();

        CheckAvailabilityRequest checkAvailabilityRequest = CheckAvailabilityRequest.newBuilder()
                .addAllProductItems(items)
                .build();

        CheckAvailabilityResponse response = stub.checkAvailability(checkAvailabilityRequest);
        return response.getProductAvailabilitiesList();
    }

    public void writeOffProducts(List<com.example.orderservice.dto.ProductItem> items, UUID orderId) {
        List<ProductItem> productItems = items.stream()
                .map(item -> ProductItem.newBuilder()
                        .setId(item.id())
                        .setQuantity(item.quantity())
                        .build())
                .toList();

        WriteOffRequest writeOffRequest = WriteOffRequest.newBuilder()
                .addAllItems(productItems)
                .setOrderId(orderId.toString())
                .build();

        stub.writeOffProducts(writeOffRequest);
    }
}
