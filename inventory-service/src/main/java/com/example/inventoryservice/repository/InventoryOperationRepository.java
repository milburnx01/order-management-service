package com.example.inventoryservice.repository;

import com.example.inventoryservice.entity.InventoryOperation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface InventoryOperationRepository extends JpaRepository<InventoryOperation, UUID> {

    @Modifying
    @Query(value = """
    INSERT INTO inventory_operations (order_id)
    VALUES (:orderId)
    ON CONFLICT (order_id) DO NOTHING
    """, nativeQuery = true)
    int registerOperation(@Param("orderId") UUID orderId);

}
