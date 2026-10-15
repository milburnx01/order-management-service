package com.example.notificationservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "orders")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "order_id")
    private UUID orderId;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "product_id")
    private UUID productId;

    @Column(name = "quantity")
    private Integer quantity;

    @Column(name = "price", precision = 19, scale = 2)
    private BigDecimal price;

    @Column(name = "sale", precision = 19, scale = 2)
    private BigDecimal sale;

    @Column(name = "total_price", precision = 19, scale = 2)
    private BigDecimal totalPrice;
}
