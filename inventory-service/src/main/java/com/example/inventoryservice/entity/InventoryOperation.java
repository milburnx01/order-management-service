package com.example.inventoryservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "inventory_operations")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class InventoryOperation {

    @Id
    @Column(name = "order_id", nullable = false)
    private UUID orderId;
}
