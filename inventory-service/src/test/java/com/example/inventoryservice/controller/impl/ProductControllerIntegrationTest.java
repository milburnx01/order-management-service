package com.example.inventoryservice.controller.impl;

import com.example.inventoryservice.entity.Product;
import com.example.inventoryservice.repository.ProductRepository;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class ProductControllerIntegrationTest {

    @Container
    private final static PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:16");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();
    }

    @Test
    void createProduct_ShouldReturnCreatedAndSaveInDatabase()
            throws Exception {
        // given
        String request = """
                {
                    "name": "Laptop",
                    "quantity": 10,
                    "price": 1000.00,
                    "sale": 10.00
                }
                """;

        // when & then
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.name").value("Laptop"))
                .andExpect(jsonPath("$.quantity").value(10))
                .andExpect(jsonPath("$.price").value(1000.00))
                .andExpect(jsonPath("$.sale").value(10.00));

        assertThat(productRepository.findAll())
                .hasSize(1)
                .first()
                .satisfies(product -> {
                    assertThat(product.getName()).isEqualTo("Laptop");
                    assertThat(product.getQuantity()).isEqualTo(10);
                    assertThat(product.getPrice())
                            .isEqualByComparingTo("1000.00");
                });
    }

    @Test
    @SneakyThrows
    void findById_ShouldReturnProduct_WhenProductExists() {
        // given
        Product product = productRepository.save(
                new Product(
                        null,
                        "Phone",
                        5,
                        new BigDecimal("500.00"),
                        new BigDecimal("5.00")
                )
        );

        // when & then
        mockMvc.perform(get("/api/products/{productId}", product.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id")
                        .value(product.getId().toString()))
                .andExpect(jsonPath("$.name").value("Phone"))
                .andExpect(jsonPath("$.quantity").value(5))
                .andExpect(jsonPath("$.price").value(500.00))
                .andExpect(jsonPath("$.sale").value(5.00));
    }

    @Test
    @SneakyThrows
    void findById_ShouldReturnNotFound_WhenProductDoesNotExist() {
        // given
        UUID productId = UUID.randomUUID();

        // when & then
        mockMvc.perform(get("/api/products/{productId}", productId))
                .andExpect(status().isNotFound());
    }

    @Test
    @SneakyThrows
    void findAll_ShouldReturnPageOfProducts() {
        // given
        productRepository.save(
                new Product(
                        null,
                        "Laptop",
                        10,
                        new BigDecimal("1000.00"),
                        BigDecimal.ZERO
                )
        );

        productRepository.save(
                new Product(
                        null,
                        "Mouse",
                        20,
                        new BigDecimal("100.00"),
                        BigDecimal.ZERO
                )
        );

        // when & then
        mockMvc.perform(get("/api/products")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.number").value(0));
    }

    @Test
    @SneakyThrows
    void deleteProduct_ShouldReturnNoContentAndDeleteFromDatabase() {
        // given
        Product product = productRepository.save(
                new Product(
                        null,
                        "Keyboard",
                        5,
                        new BigDecimal("150.00"),
                        BigDecimal.ZERO
                )
        );

        UUID productId = product.getId();

        // when & then
        mockMvc.perform(delete("/api/products/{productId}", productId))
                .andExpect(status().isNoContent());

        assertThat(productRepository.existsById(productId)).isFalse();
    }
}
