package com.example.notificationservice.repository;

import com.example.notificationservice.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

    Page<Order> findAll(Pageable pageable);

    Page<Order> findAllByOrderId(UUID orderId, Pageable pageable);

    Page<Order> findAllByUserId(UUID userId, Pageable pageable);
}
