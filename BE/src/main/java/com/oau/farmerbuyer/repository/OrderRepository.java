package com.oau.farmerbuyer.repository;

import java.util.Optional;
import com.oau.farmerbuyer.domain.AppUser;


import com.oau.farmerbuyer.domain.AppUser;
import com.oau.farmerbuyer.domain.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
public interface OrderRepository extends JpaRepository<Order, Long> {
    Page<Order> findByBuyer(AppUser buyer, Pageable pageable);

    Optional<Order> findByBuyerAndIdempotencyKey(AppUser buyer, String idempotencyKey);
}


