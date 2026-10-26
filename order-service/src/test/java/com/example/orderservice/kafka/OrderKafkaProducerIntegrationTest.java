package com.example.orderservice.kafka;

import com.example.orderservice.dto.OrderItemResponse;
import com.example.orderservice.dto.OrderResponse;
import com.jayway.jsonpath.JsonPath;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class OrderKafkaProducerIntegrationTest {

    @Container
    private static final KafkaContainer kafka =
            new KafkaContainer("apache/kafka-native:4.0.0");

    @Container
    private static final PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:16");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private OrderKafkaProducer orderKafkaProducer;

    @Autowired
    private KafkaTemplate<String, OrderResponse> kafkaTemplate;

    private KafkaConsumer<String, String> consumer;

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
        consumer = new KafkaConsumer<>(Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                kafka.getBootstrapServers(),
                ConsumerConfig.GROUP_ID_CONFIG,
                "orders-test-" + UUID.randomUUID(),
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                "latest",
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class
        ));

        consumer.subscribe(List.of("orders"));

        await().atMost(Duration.ofSeconds(10)).until(() -> {
            consumer.poll(Duration.ofMillis(100));
            return !consumer.assignment().isEmpty();
        });
    }

    @AfterEach
    void tearDown() {
        consumer.close();
    }

    @Test
    void sendOrder_ShouldPublishMessageToKafka() {
        // given
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        OrderResponse order = OrderResponse.builder()
                .orderId(orderId)
                .userId(userId)
                .items(List.of())
                .totalPrice(BigDecimal.ZERO)
                .build();

        // when
        orderKafkaProducer.sendOrder(order);
        kafkaTemplate.flush();

        ConsumerRecord<String, String> record =
                KafkaTestUtils.getSingleRecord(
                        consumer,
                        "orders",
                        Duration.ofSeconds(15)
                );

        // then
        assertThat(record.topic()).isEqualTo("orders");
        assertThat(record.value()).isNotBlank();

        assertThat(JsonPath.<String>read(record.value(), "$.orderId"))
                .isEqualTo(orderId.toString());

        assertThat(JsonPath.<String>read(record.value(), "$.userId"))
                .isEqualTo(userId.toString());
    }

    @Test
    void sendOrder_ShouldPreserveOrderData() {
        // given
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        OrderItemResponse item = OrderItemResponse.builder()
                .productId(productId)
                .productName("Laptop")
                .quantity(2)
                .price(new BigDecimal("1000.00"))
                .sale(new BigDecimal("10.00"))
                .totalPrice(new BigDecimal("1800.00"))
                .build();

        OrderResponse order = OrderResponse.builder()
                .orderId(orderId)
                .userId(userId)
                .items(List.of(item))
                .totalPrice(new BigDecimal("1800.00"))
                .build();

        // when
        orderKafkaProducer.sendOrder(order);
        kafkaTemplate.flush();

        ConsumerRecord<String, String> record =
                KafkaTestUtils.getSingleRecord(
                        consumer,
                        "orders",
                        Duration.ofSeconds(15)
                );

        // then
        String json = record.value();

        assertThat(JsonPath.<String>read(json, "$.orderId"))
                .isEqualTo(orderId.toString());

        assertThat(JsonPath.<String>read(json, "$.userId"))
                .isEqualTo(userId.toString());

        assertThat(JsonPath.<String>read(json, "$.items[0].productId"))
                .isEqualTo(productId.toString());

        assertThat(JsonPath.<String>read(json, "$.items[0].productName"))
                .isEqualTo("Laptop");

        assertThat(JsonPath.<Integer>read(json, "$.items[0].quantity"))
                .isEqualTo(2);

        assertThat(new BigDecimal(
                JsonPath.read(json, "$.items[0].price").toString()
        )).isEqualByComparingTo("1000.00");

        assertThat(new BigDecimal(
                JsonPath.read(json, "$.items[0].sale").toString()
        )).isEqualByComparingTo("10.00");

        assertThat(new BigDecimal(
                JsonPath.read(json, "$.items[0].totalPrice").toString()
        )).isEqualByComparingTo("1800.00");

        assertThat(new BigDecimal(
                JsonPath.read(json, "$.totalPrice").toString()
        )).isEqualByComparingTo("1800.00");
    }

    @Test
    void sendOrder_ShouldPublishMultipleOrders() {
        // given
        UUID firstOrderId = UUID.randomUUID();
        UUID secondOrderId = UUID.randomUUID();

        OrderResponse firstOrder = OrderResponse.builder()
                .orderId(firstOrderId)
                .userId(UUID.randomUUID())
                .items(List.of())
                .totalPrice(new BigDecimal("100.00"))
                .build();

        OrderResponse secondOrder = OrderResponse.builder()
                .orderId(secondOrderId)
                .userId(UUID.randomUUID())
                .items(List.of())
                .totalPrice(new BigDecimal("200.00"))
                .build();

        // when
        orderKafkaProducer.sendOrder(firstOrder);
        orderKafkaProducer.sendOrder(secondOrder);
        kafkaTemplate.flush();

        List<ConsumerRecord<String, String>> receivedRecords =
                new ArrayList<>();

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            ConsumerRecords<String, String> records =
                    consumer.poll(Duration.ofMillis(500));

            records.forEach(receivedRecords::add);

            assertThat(receivedRecords).hasSize(2);
        });

        // then
        assertThat(receivedRecords)
                .extracting(record ->
                        JsonPath.<String>read(record.value(), "$.orderId"))
                .containsExactlyInAnyOrder(
                        firstOrderId.toString(),
                        secondOrderId.toString()
                );

        assertThat(receivedRecords)
                .extracting(ConsumerRecord::topic)
                .containsOnly("orders");
    }
}
