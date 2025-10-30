// src/main/java/com/oau/farmerbuyer/service/OrderService.java
package com.oau.farmerbuyer.service;

import com.oau.farmerbuyer.dto.OrderDtos;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OrderService {

    /**
     * Create a PENDING order (idempotent via header).
     * buyerId comes from Authentication (controller) and is passed here.
     */
    OrderDtos.Response createPendingOrder(Long buyerId, OrderDtos.Create req, String idemKey);

    // List and get remain the same
    Page<OrderDtos.Response> myOrders(Long buyerId, Pageable pageable);
    OrderDtos.Response getOne(Long id, Long buyerId);

    /** @deprecated Legacy entry point; keep temporarily if other callers still use it */
    @Deprecated
    default OrderDtos.Response place(OrderDtos.Create dto, String idemKey) {
        throw new UnsupportedOperationException("Use createPendingOrder(buyerId, req, idemKey)");
    }
}
