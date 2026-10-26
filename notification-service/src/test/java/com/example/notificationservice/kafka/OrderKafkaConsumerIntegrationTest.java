package com.example.notificationservice.kafka;

import com.example.notificationservice.dto.OrderCreatedEvent;
import com.example.notificationservice.dto.OrderItem;
import com.example.notificationservice.entity.Order;
import com.example.notificationservice.repository.OrderRepository;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class OrderKafkaConsumerIntegrationTest {

    @Container
    private static final KafkaContainer kafka =
            new KafkaContainer("apache/kafka-native:4.0.0");

    @Container
    private static final PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:16");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers",
                kafka::getBootstrapServers);

        registry.add("spring.datasource.url",
                postgres::getJdbcUrl);
        registry.add("spring.datasource.username",
                postgres::getUsername);
        registry.add("spring.datasource.password",
                postgres::getPassword);

        registry.add("spring.kafka.consumer.group-id",
                () -> "notification-test-" + UUID.randomUUID());

        registry.add("spring.kafka.consumer.auto-offset-reset",
                () -> "earliest");

        registry.add("spring.kafka.producer.key-serializer",
                () -> "org.apache.kafka.common.serialization.StringSerializer");

        registry.add("spring.kafka.producer.value-serializer",
                () -> "org.springframework.kafka.support.serializer.JsonSerializer");

        registry.add("spring.kafka.producer.properties[spring.json.add.type.headers]",
                () -> "false");
    }

    @Autowired
    private KafkaTemplate<String, OrderCreatedEvent> kafkaTemplate;

    @Autowired
    private OrderRepository orderRepository;

    @BeforeAll
    static void createTopic() throws Exception {
        try (AdminClient admin = AdminClient.create(
                Map.of("bootstrap.servers", kafka.getBootstrapServers()))) {

            admin.createTopics(List.of(
                    new NewTopic("orders", 1, (short) 1)
            )).all().get();
        }
    }

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
    }

    @Test
    void consumeOrder_ShouldSaveOrder_WhenEventContainsOneItem()
            throws Exception {

        // given
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        OrderItem item = new OrderItem(
                productId,
                "Laptop",
                2,
                new BigDecimal("1000.00"),
                new BigDecimal("10.00"),
                new BigDecimal("1800.00")
        );

        OrderCreatedEvent event = new OrderCreatedEvent(
                orderId,
                userId,
                List.of(item),
                new BigDecimal("1800.00")
        );

        // when
        kafkaTemplate.send("orders", event)
                .get(10, TimeUnit.SECONDS);

        // then
        await()
                .atMost(Duration.ofSeconds(15))
                .untilAsserted(() -> {
                    List<Order> savedOrders = orderRepository
                            .findAllByOrderId(
                                    orderId,
                                    Pageable.unpaged()
                            )
                            .getContent();

                    assertThat(savedOrders).hasSize(1);

                    Order savedOrder = savedOrders.getFirst();

                    assertThat(savedOrder.getId()).isNotNull();
                    assertThat(savedOrder.getOrderId())
                            .isEqualTo(orderId);
                    assertThat(savedOrder.getUserId())
                            .isEqualTo(userId);
                    assertThat(savedOrder.getProductId())
                            .isEqualTo(productId);
                    assertThat(savedOrder.getQuantity())
                            .isEqualTo(2);

                    assertThat(savedOrder.getPrice())
                            .isEqualByComparingTo("1000.00");

                    assertThat(savedOrder.getSale())
                            .isEqualByComparingTo("10.00");

                    assertThat(savedOrder.getTotalPrice())
                            .isEqualByComparingTo("1800.00");
                });
    }

    @Test
    void consumeOrder_ShouldSaveAllItems_WhenEventContainsMultipleItems()
            throws Exception {

        // given
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID firstProductId = UUID.randomUUID();
        UUID secondProductId = UUID.randomUUID();

        OrderItem firstItem = new OrderItem(
                firstProductId,
                "Laptop",
                1,
                new BigDecimal("1000.00"),
                BigDecimal.ZERO,
                new BigDecimal("1000.00")
        );

        OrderItem secondItem = new OrderItem(
                secondProductId,
                "Mouse",
                2,
                new BigDecimal("50.00"),
                BigDecimal.ZERO,
                new BigDecimal("100.00")
        );

        OrderCreatedEvent event = new OrderCreatedEvent(
                orderId,
                userId,
                List.of(firstItem, secondItem),
                new BigDecimal("1100.00")
        );

        // when
        kafkaTemplate.send("orders", event)
                .get(10, TimeUnit.SECONDS);

        // then
        await()
                .atMost(Duration.ofSeconds(15))
                .untilAsserted(() -> {
                    List<Order> savedOrders = orderRepository
                            .findAllByOrderId(
                                    orderId,
                                    Pageable.unpaged()
                            )
                            .getContent();

                    assertThat(savedOrders).hasSize(2);

                    assertThat(savedOrders)
                            .extracting(Order::getProductId)
                            .containsExactlyInAnyOrder(
                                    firstProductId,
                                    secondProductId
                            );

                    assertThat(savedOrders)
                            .extracting(Order::getUserId)
                            .containsOnly(userId);

                    assertThat(savedOrders)
                            .extracting(Order::getQuantity)
                            .containsExactlyInAnyOrder(1, 2);

                    assertThat(savedOrders)
                            .extracting(Order::getTotalPrice)
                            .containsExactlyInAnyOrder(
                                    new BigDecimal("1000.00"),
                                    new BigDecimal("100.00")
                            );
                });
    }

    @Test
    void consumeOrder_ShouldPreserveOrderData()
            throws Exception {

        // given
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        OrderItem item = new OrderItem(
                productId,
                "Phone",
                3,
                new BigDecimal("500.00"),
                new BigDecimal("20.00"),
                new BigDecimal("1200.00")
        );

        OrderCreatedEvent event = new OrderCreatedEvent(
                orderId,
                userId,
                List.of(item),
                new BigDecimal("1200.00")
        );

        // when
        kafkaTemplate.send("orders", event)
                .get(10, TimeUnit.SECONDS);

        // then
        await()
                .atMost(Duration.ofSeconds(15))
                .untilAsserted(() -> {
                    List<Order> savedOrders = orderRepository
                            .findAllByOrderId(
                                    orderId,
                                    Pageable.unpaged()
                            )
                            .getContent();

                    assertThat(savedOrders).hasSize(1);

                    Order savedOrder = savedOrders.getFirst();

                    assertThat(savedOrder.getOrderId())
                            .isEqualTo(orderId);

                    assertThat(savedOrder.getUserId())
                            .isEqualTo(userId);

                    assertThat(savedOrder.getProductId())
                            .isEqualTo(productId);

                    assertThat(savedOrder.getQuantity())
                            .isEqualTo(3);

                    assertThat(savedOrder.getPrice())
                            .isEqualByComparingTo("500.00");

                    assertThat(savedOrder.getSale())
                            .isEqualByComparingTo("20.00");

                    assertThat(savedOrder.getTotalPrice())
                            .isEqualByComparingTo("1200.00");
                });
    }
}
