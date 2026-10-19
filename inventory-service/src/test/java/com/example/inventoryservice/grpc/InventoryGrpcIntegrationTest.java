
package com.example.inventoryservice.grpc;

import com.example.inventoryservice.entity.Product;
import com.example.inventoryservice.repository.ProductRepository;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.grpc.test.autoconfigure.LocalGrpcServerPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = "spring.grpc.server.port=0")
@Testcontainers
class InventoryGrpcIntegrationTest {

    @Container
    private static final PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:16");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @LocalGrpcServerPort
    private int grpcPort;

    @Autowired
    private ProductRepository productRepository;

    private ManagedChannel channel;
    private InventoryServiceGrpc.InventoryServiceBlockingStub stub;

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();

        channel = ManagedChannelBuilder
                .forAddress("localhost", grpcPort)
                .usePlaintext()
                .build();

        stub = InventoryServiceGrpc.newBlockingStub(channel);
    }

    @AfterEach
    void tearDown() {
        if (channel != null) {
            channel.shutdownNow();
        }
    }

    @Test
    void checkAvailability_ShouldReturnAvailable_WhenStockIsSufficient() {
        // given
        Product product = productRepository.saveAndFlush(
                new Product(
                        null,
                        "Laptop",
                        10,
                        new BigDecimal("1000.00"),
                        new BigDecimal("15.00")
                )
        );

        CheckAvailabilityRequest request = CheckAvailabilityRequest.newBuilder()
                .addProductItems(ProductItem.newBuilder()
                        .setId(product.getId().toString())
                        .setQuantity(3)
                        .build())
                .build();

        // when
        CheckAvailabilityResponse response =
                stub.checkAvailability(request);

        // then
        assertThat(response.getProductAvailabilitiesCount()).isEqualTo(1);

        ProductAvailability availability =
                response.getProductAvailabilities(0);

        assertThat(availability.getId())
                .isEqualTo(product.getId().toString());
        assertThat(availability.getName()).isEqualTo("Laptop");
        assertThat(availability.getPrice()).isEqualTo(1000.0);
        assertThat(availability.getSale()).isEqualTo(15.0);
        assertThat(availability.getAvailableQuantity()).isEqualTo(10);
        assertThat(availability.getIsAvailable()).isTrue();
    }

    @Test
    void checkAvailability_ShouldReturnUnavailable_WhenStockIsInsufficient() {
        // given
        Product product = productRepository.saveAndFlush(
                new Product(
                        null,
                        "Keyboard",
                        2,
                        new BigDecimal("200.00"),
                        BigDecimal.ZERO
                )
        );

        CheckAvailabilityRequest request = CheckAvailabilityRequest.newBuilder()
                .addProductItems(ProductItem.newBuilder()
                        .setId(product.getId().toString())
                        .setQuantity(5)
                        .build())
                .build();

        // when
        CheckAvailabilityResponse response =
                stub.checkAvailability(request);

        // then
        assertThat(response.getProductAvailabilitiesCount()).isEqualTo(1);

        ProductAvailability availability =
                response.getProductAvailabilities(0);

        assertThat(availability.getIsAvailable()).isFalse();
        assertThat(availability.getAvailableQuantity()).isEqualTo(2);

        assertThat(productRepository.findById(product.getId())
                .orElseThrow()
                .getQuantity()).isEqualTo(2);
    }

    @Test
    void checkAvailability_ShouldReturnUnavailable_WhenProductDoesNotExist() {
        // given
        UUID productId = UUID.randomUUID();

        CheckAvailabilityRequest request = CheckAvailabilityRequest.newBuilder()
                .addProductItems(ProductItem.newBuilder()
                        .setId(productId.toString())
                        .setQuantity(1)
                        .build())
                .build();

        // when
        CheckAvailabilityResponse response =
                stub.checkAvailability(request);

        // then
        assertThat(response.getProductAvailabilitiesCount()).isEqualTo(1);

        ProductAvailability availability =
                response.getProductAvailabilities(0);

        assertThat(availability.getId()).isEqualTo(productId.toString());
        assertThat(availability.getIsAvailable()).isFalse();
        assertThat(availability.getAvailableQuantity()).isZero();
    }

    @Test
    void writeOffProducts_ShouldDecreaseQuantity() {
        // given
        Product product = productRepository.saveAndFlush(
                new Product(
                        null,
                        "Mouse",
                        10,
                        new BigDecimal("100.00"),
                        BigDecimal.ZERO
                )
        );

        WriteOffRequest request = WriteOffRequest.newBuilder()
                .addItems(ProductItem.newBuilder()
                        .setId(product.getId().toString())
                        .setQuantity(4)
                        .build())
                .build();

        // when
        stub.writeOffProducts(request);

        // then
        Product updatedProduct = productRepository.findById(product.getId())
                .orElseThrow();

        assertThat(updatedProduct.getQuantity()).isEqualTo(6);
    }

    @Test
    void writeOffProducts_ShouldReturnNotFound_WhenProductDoesNotExist() {
        // given
        UUID productId = UUID.randomUUID();

        WriteOffRequest request = WriteOffRequest.newBuilder()
                .addItems(ProductItem.newBuilder()
                        .setId(productId.toString())
                        .setQuantity(2)
                        .build())
                .build();

        // when / then
        assertThatThrownBy(() -> stub.writeOffProducts(request))
                .isInstanceOf(StatusRuntimeException.class)
                .satisfies(exception -> {
                    StatusRuntimeException grpcException =
                            (StatusRuntimeException) exception;

                    assertThat(grpcException.getStatus().getCode())
                            .isEqualTo(Status.Code.NOT_FOUND);

                    assertThat(grpcException.getStatus().getDescription())
                            .contains(productId.toString());
                });
    }

    @Test
    void writeOffProducts_ShouldRollback_WhenOneProductDoesNotExist() {
        // given
        Product product = productRepository.saveAndFlush(
                new Product(
                        null,
                        "Monitor",
                        10,
                        new BigDecimal("500.00"),
                        BigDecimal.ZERO
                )
        );

        UUID missingProductId = UUID.randomUUID();

        WriteOffRequest request = WriteOffRequest.newBuilder()
                .addItems(ProductItem.newBuilder()
                        .setId(product.getId().toString())
                        .setQuantity(4)
                        .build())
                .addItems(ProductItem.newBuilder()
                        .setId(missingProductId.toString())
                        .setQuantity(1)
                        .build())
                .build();

        // when
        assertThatThrownBy(() -> stub.writeOffProducts(request))
                .isInstanceOf(StatusRuntimeException.class)
                .satisfies(exception -> {
                    StatusRuntimeException grpcException =
                            (StatusRuntimeException) exception;

                    assertThat(grpcException.getStatus().getCode())
                            .isEqualTo(Status.Code.NOT_FOUND);
                });

        // then
        Product unchangedProduct = productRepository.findById(product.getId())
                .orElseThrow();

        assertThat(unchangedProduct.getQuantity()).isEqualTo(10);
    }
}
