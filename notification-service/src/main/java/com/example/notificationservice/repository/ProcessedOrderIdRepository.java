package com.example.notificationservice.repository;

import com.example.notificationservice.entity.ProcessedOrderId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ProcessedOrderIdRepository extends JpaRepository<ProcessedOrderId, UUID> {

    @Modifying
    @Query(value = """
            INSERT INTO processed_order_id (order_id)
            VALUES (:orderId)
            ON CONFLICT (order_id) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(@Param("orderId") UUID orderId);
}