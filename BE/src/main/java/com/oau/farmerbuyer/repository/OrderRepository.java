// src/main/java/com/oau/farmerbuyer/repository/OrderRepository.java
package com.oau.farmerbuyer.repository;

import com.oau.farmerbuyer.domain.AppUser;
import com.oau.farmerbuyer.domain.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    Page<Order> findByBuyer(AppUser buyer, Pageable pageable);

    Optional<Order> findByBuyerAndIdempotencyKey(AppUser buyer, String idempotencyKey);

    // ⭐ New buyer-scoped helpers
    Page<Order> findByBuyer_IdOrderByCreatedAtDesc(Long buyerId, Pageable pageable);

    Optional<Order> findByIdAndBuyer_Id(Long id, Long buyerId);
}
