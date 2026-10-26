
package com.example.inventoryservice.service.impl;

import com.example.inventoryservice.dto.CreateProductRequest;
import com.example.inventoryservice.dto.ProductResponse;
import com.example.inventoryservice.entity.Product;
import com.example.inventoryservice.exception.ProductNotFoundException;
import com.example.inventoryservice.grpc.ProductAvailability;
import com.example.inventoryservice.grpc.ProductItem;
import com.example.inventoryservice.mapper.ProductMapper;
import com.example.inventoryservice.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductMapper productMapper;

    @InjectMocks
    private ProductServiceImpl productService;

    @Test
    void findAll_ShouldReturnPageOfProducts() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        UUID productId = UUID.randomUUID();

        Product product = new Product(
                productId,
                "Laptop",
                10,
                new BigDecimal("1000.00"),
                new BigDecimal("10.00")
        );

        ProductResponse response = new ProductResponse(
                productId,
                "Laptop",
                10,
                new BigDecimal("1000.00"),
                new BigDecimal("10.00")
        );

        when(productRepository.findAll(pageable))
                .thenReturn(new PageImpl<>(List.of(product)));

        when(productMapper.toProductResponse(product))
                .thenReturn(response);

        // when
        Page<ProductResponse> result = productService.findAll(pageable);

        // then
        assertThat(result.getContent()).containsExactly(response);
        assertThat(result.getTotalElements()).isEqualTo(1);

        verify(productRepository).findAll(pageable);
        verify(productMapper).toProductResponse(product);
    }

    @Test
    void findById_ShouldReturnProduct_WhenProductExists() {
        // given
        UUID productId = UUID.randomUUID();

        Product product = new Product(
                productId,
                "Laptop",
                10,
                new BigDecimal("1000.00"),
                new BigDecimal("10.00")
        );

        ProductResponse response = new ProductResponse(
                productId,
                "Laptop",
                10,
                new BigDecimal("1000.00"),
                new BigDecimal("10.00")
        );

        when(productRepository.findById(productId))
                .thenReturn(Optional.of(product));

        when(productMapper.toProductResponse(product))
                .thenReturn(response);

        // when
        ProductResponse result = productService.findById(productId);

        // then
        assertThat(result).isEqualTo(response);

        verify(productRepository).findById(productId);
        verify(productMapper).toProductResponse(product);
    }

    @Test
    void findById_ShouldThrowException_WhenProductNotFound() {
        // given
        UUID productId = UUID.randomUUID();

        when(productRepository.findById(productId))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> productService.findById(productId))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessage("Product with id " + productId + " not found");

        verify(productRepository).findById(productId);
        verify(productMapper, never())
                .toProductResponse(any(Product.class));
    }

    @Test
    void createProduct_ShouldReturnCreatedProduct() {
        // given
        UUID productId = UUID.randomUUID();

        CreateProductRequest request = new CreateProductRequest(
                "Laptop",
                10,
                new BigDecimal("1000.00"),
                new BigDecimal("10.00")
        );

        Product product = new Product(
                productId,
                "Laptop",
                10,
                new BigDecimal("1000.00"),
                new BigDecimal("10.00")
        );

        ProductResponse response = new ProductResponse(
                productId,
                "Laptop",
                10,
                new BigDecimal("1000.00"),
                new BigDecimal("10.00")
        );

        when(productMapper.toProduct(request))
                .thenReturn(product);

        when(productRepository.save(product))
                .thenReturn(product);

        when(productMapper.toProductResponse(product))
                .thenReturn(response);

        // when
        ProductResponse result = productService.createProduct(request);

        // then
        assertThat(result).isEqualTo(response);

        verify(productMapper).toProduct(request);
        verify(productRepository).save(product);
        verify(productMapper).toProductResponse(product);
    }

    @Test
    void deleteProduct_ShouldDeleteProduct() {
        // given
        UUID productId = UUID.randomUUID();

        // when
        productService.deleteProduct(productId);

        // then
        verify(productRepository).deleteById(productId);
    }

    @Test
    void checkAvailability_ShouldReturnTrue_WhenQuantityIsEnough() {
        // given
        UUID productId = UUID.randomUUID();

        Product product = new Product(
                productId,
                "Laptop",
                10,
                new BigDecimal("1000.00"),
                new BigDecimal("10.00")
        );

        ProductItem item = ProductItem.newBuilder()
                .setId(productId.toString())
                .setQuantity(5)
                .build();

        when(productRepository.findById(productId))
                .thenReturn(Optional.of(product));

        // when
        List<ProductAvailability> result =
                productService.checkAvailability(List.of(item));

        // then
        assertThat(result).hasSize(1);

        ProductAvailability availability = result.getFirst();

        assertThat(availability.getIsAvailable()).isTrue();
        assertThat(availability.getId()).isEqualTo(productId.toString());
        assertThat(availability.getName()).isEqualTo("Laptop");
        assertThat(availability.getAvailableQuantity()).isEqualTo(10);
        assertThat(availability.getPrice()).isEqualTo(1000.0);
        assertThat(availability.getSale()).isEqualTo(10.0);

        verify(productRepository).findById(productId);
    }

    @Test
    void checkAvailability_ShouldReturnFalse_WhenQuantityIsNotEnough() {
        // given
        UUID productId = UUID.randomUUID();

        Product product = new Product(
                productId,
                "Laptop",
                10,
                new BigDecimal("1000.00"),
                new BigDecimal("10.00")
        );

        ProductItem item = ProductItem.newBuilder()
                .setId(productId.toString())
                .setQuantity(20)
                .build();

        when(productRepository.findById(productId))
                .thenReturn(Optional.of(product));

        // when
        List<ProductAvailability> result =
                productService.checkAvailability(List.of(item));

        // then
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getIsAvailable()).isFalse();
        assertThat(result.getFirst().getAvailableQuantity()).isEqualTo(10);

        verify(productRepository).findById(productId);
    }

    @Test
    void checkAvailability_ShouldReturnFalse_WhenProductNotFound() {
        // given
        UUID productId = UUID.randomUUID();

        ProductItem item = ProductItem.newBuilder()
                .setId(productId.toString())
                .setQuantity(5)
                .build();

        when(productRepository.findById(productId))
                .thenReturn(Optional.empty());

        // when
        List<ProductAvailability> result =
                productService.checkAvailability(List.of(item));

        // then
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getId()).isEqualTo(productId.toString());
        assertThat(result.getFirst().getIsAvailable()).isFalse();

        verify(productRepository).findById(productId);
    }

    @Test
    void writeOffProducts_ShouldDecreaseProductQuantity() {
        // given
        UUID productId = UUID.randomUUID();

        Product product = new Product(
                productId,
                "Laptop",
                10,
                new BigDecimal("1000.00"),
                new BigDecimal("10.00")
        );

        ProductItem item = ProductItem.newBuilder()
                .setId(productId.toString())
                .setQuantity(3)
                .build();

        when(productRepository.findById(productId))
                .thenReturn(Optional.of(product));

        // when
        productService.writeOffProducts(List.of(item));

        // then
        assertThat(product.getQuantity()).isEqualTo(7);

        verify(productRepository).findById(productId);
    }

    @Test
    void writeOffProducts_ShouldThrowException_WhenProductNotFound() {
        // given
        UUID productId = UUID.randomUUID();

        ProductItem item = ProductItem.newBuilder()
                .setId(productId.toString())
                .setQuantity(3)
                .build();

        when(productRepository.findById(productId))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(
                () -> productService.writeOffProducts(List.of(item))
        )
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessage("Product with id " + productId + " not found");

        verify(productRepository).findById(productId);
    }
}
