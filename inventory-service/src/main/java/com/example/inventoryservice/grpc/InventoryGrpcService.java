package com.example.inventoryservice.grpc;

import com.example.inventoryservice.service.ProductService;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import org.springframework.grpc.server.service.GrpcService;

import java.util.List;

@GrpcService
@RequiredArgsConstructor
public class InventoryGrpcService extends InventoryServiceGrpc.InventoryServiceImplBase {

    private final ProductService productService;

    @Override
    public void checkAvailability(CheckAvailabilityRequest request, StreamObserver<CheckAvailabilityResponse> responseObserver) {
        List<ProductAvailability> productAvailabilities = productService.checkAvailability(request.getProductItemsList());
        CheckAvailabilityResponse response = CheckAvailabilityResponse.newBuilder()
                .addAllProductAvailabilities(productAvailabilities)
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
